import json

from app.consumidor import Decisao, declarar_topologia, processar_mensagem
from app.notificacoes import RegistroNotificacoes


def mensagem(**campos) -> bytes:
    evento = {"eventoId": "e1", "tipo": "AlunoPromovidoPremium", "alunoId": 3,
              "cursosConcluidos": 12, "ocorridoEm": "2026-11-03T12:00:00Z"}
    evento.update(campos)
    return json.dumps(evento).encode("utf-8")


def test_notifica_avisa_o_core_e_confirma():
    registro = RegistroNotificacoes()
    avisados = []

    decisao = processar_mensagem(mensagem(), registro, lambda aluno_id: avisados.append(aluno_id) or True)

    assert decisao is Decisao.ACK
    assert avisados == [3]
    assert len(registro.listar()) == 1


def test_core_fora_do_ar_devolve_para_a_fila_sem_perder_a_mensagem():
    registro = RegistroNotificacoes()

    assert processar_mensagem(mensagem(), registro, lambda _id: False) is Decisao.REENFILEIRAR


def test_reentrega_da_mesma_mensagem_nao_duplica_a_notificacao():
    registro = RegistroNotificacoes()

    processar_mensagem(mensagem(), registro, lambda _id: False)   # 1a entrega: Core fora
    decisao = processar_mensagem(mensagem(), registro, lambda _id: True)  # 2a entrega: Core de volta

    assert decisao is Decisao.ACK
    assert len(registro.listar()) == 1


def test_mensagem_invalida_e_descartada():
    registro = RegistroNotificacoes()

    assert processar_mensagem(b"isso nao e json", registro, lambda _id: True) is Decisao.DESCARTAR
    assert processar_mensagem(b'{"eventoId":"x"}', registro, lambda _id: True) is Decisao.DESCARTAR
    assert registro.listar() == []


class CanalFalso:
    def __init__(self):
        self.chamadas = []

    def __getattr__(self, nome):
        return lambda **kwargs: self.chamadas.append((nome, kwargs))


def test_topologia_igual_a_declarada_pelo_core():
    canal = CanalFalso()

    declarar_topologia(canal)

    assert ("exchange_declare", {"exchange": "gamificacao.eventos", "exchange_type": "topic", "durable": True}) in canal.chamadas
    assert ("queue_declare", {"queue": "notificacao.aluno-promovido", "durable": True}) in canal.chamadas
    assert ("queue_bind", {"queue": "notificacao.aluno-promovido", "exchange": "gamificacao.eventos",
                           "routing_key": "aluno.promovido-premium"}) in canal.chamadas