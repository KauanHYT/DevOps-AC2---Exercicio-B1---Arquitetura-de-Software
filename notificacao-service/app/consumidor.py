"""Consumidor RabbitMQ do evento AlunoPromovidoPremium (ADR-004).

Fluxo de cada mensagem:
  1. envia a notificação (idempotente por eventoId);
  2. avisa o Core: PUT /alunos/{id}/notificar (endpoint idempotente da US 3);
  3. só então confirma (ack). Se o Core não responder, devolve a mensagem para a fila.

A topologia (exchange, fila, binding) é declarada aqui e também no Core, com os
mesmos parâmetros, para que nenhum dos dois dependa da ordem de inicialização.
"""
import json
import logging
import os
import time
import urllib.error
import urllib.request
from enum import Enum
from typing import Callable

from .notificacoes import RegistroNotificacoes

log = logging.getLogger("consumidor")

EXCHANGE = "gamificacao.eventos"
FILA = "notificacao.aluno-promovido"
ROTA = "aluno.promovido-premium"


class Decisao(Enum):
    ACK = "ack"                # processada: remove da fila
    REENFILEIRAR = "requeue"   # falha temporária (Core fora): tenta de novo depois
    DESCARTAR = "descartar"    # mensagem inválida: não adianta tentar de novo


def processar_mensagem(corpo: bytes, registro: RegistroNotificacoes,
                       avisar_core: Callable[[int], bool]) -> Decisao:
    try:
        evento = json.loads(corpo.decode("utf-8"))
        aluno_id = int(evento["alunoId"])
        str(evento["eventoId"])
    except (ValueError, KeyError, TypeError, UnicodeDecodeError) as e:
        log.error("mensagem inválida descartada (%s): %r", e, corpo[:200])
        return Decisao.DESCARTAR

    registro.enviar(evento, canal="fila")
    if avisar_core(aluno_id):
        return Decisao.ACK
    return Decisao.REENFILEIRAR


def avisar_core_via_http(core_url: str, timeout_s: float = 3.0) -> Callable[[int], bool]:
    """PUT /alunos/{id}/notificar no Core. 404 conta como concluído (aluno removido)."""
    def avisar(aluno_id: int) -> bool:
        requisicao = urllib.request.Request(f"{core_url}/alunos/{aluno_id}/notificar", method="PUT")
        try:
            with urllib.request.urlopen(requisicao, timeout=timeout_s) as resposta:
                return 200 <= resposta.status < 300
        except urllib.error.HTTPError as e:
            if e.code == 404:
                log.warning("aluno %s não existe mais no Core; mensagem confirmada", aluno_id)
                return True
            log.warning("Core respondeu %s ao registrar a notificação do aluno %s", e.code, aluno_id)
            return False
        except (urllib.error.URLError, OSError) as e:
            log.warning("Core indisponível ao registrar a notificação do aluno %s: %s", aluno_id, e)
            return False
    return avisar


def declarar_topologia(canal) -> None:
    canal.exchange_declare(exchange=EXCHANGE, exchange_type="topic", durable=True)
    canal.queue_declare(queue=FILA, durable=True)
    canal.queue_bind(queue=FILA, exchange=EXCHANGE, routing_key=ROTA)


def consumir_para_sempre(registro: RegistroNotificacoes, estado: dict) -> None:
    """Loop com reconexão: se o broker cair, tenta de novo a cada 3 s."""
    import pika  # importado aqui para os testes da lógica não dependerem do pika

    host = os.getenv("RABBITMQ_HOST", "localhost")
    avisar = avisar_core_via_http(os.getenv("CORE_URL", "http://localhost:8080"))

    def ao_receber(canal, metodo, _propriedades, corpo):
        decisao = processar_mensagem(corpo, registro, avisar)
        if decisao is Decisao.ACK:
            canal.basic_ack(delivery_tag=metodo.delivery_tag)
        elif decisao is Decisao.DESCARTAR:
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=False)
        else:
            time.sleep(2)
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=True)

    while True:
        try:
            conexao = pika.BlockingConnection(
                pika.ConnectionParameters(host=host, heartbeat=30, blocked_connection_timeout=30))
            canal = conexao.channel()
            declarar_topologia(canal)
            canal.basic_qos(prefetch_count=1)
            canal.basic_consume(queue=FILA, on_message_callback=ao_receber)
            estado["conectado"] = True
            log.info("conectado ao RabbitMQ em %s; consumindo a fila %s", host, FILA)
            canal.start_consuming()
        except Exception as e:  # noqa: BLE001 - qualquer falha de conexão leva à reconexão
            estado["conectado"] = False
            log.warning("sem conexão com o RabbitMQ (%s); nova tentativa em 3 s", e)
            time.sleep(3)