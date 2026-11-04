"""Envio e registro das notificações de promoção para PREMIUM (US 3, RF05).

O "envio" é simulado (log + registro em memória). O ponto do Desafio 4 é o canal
(fila ou HTTP) e o comportamento sob falha, não o provedor de e-mail/push.

A entrega pela fila é "pelo menos uma vez" (at-least-once): a mesma mensagem pode
chegar duas vezes. Por isso o registro é idempotente por `eventoId`.
"""
import logging
import threading
from datetime import datetime, timezone

log = logging.getLogger("notificacao")


class RegistroNotificacoes:
    def __init__(self) -> None:
        self._por_evento: dict[str, dict] = {}
        self._trava = threading.Lock()

    def enviar(self, evento: dict, canal: str) -> bool:
        """Envia a notificação do evento. Devolve False se o evento já tinha sido notificado."""
        evento_id = str(evento["eventoId"])
        with self._trava:
            if evento_id in self._por_evento:
                log.info("evento %s repetido: notificação já enviada, nada a fazer", evento_id)
                return False
            cursos = evento.get("cursosConcluidos", 12)
            notificacao = {
                "eventoId": evento_id,
                "alunoId": int(evento["alunoId"]),
                "canal": canal,
                "enviadaEm": datetime.now(timezone.utc).isoformat(timespec="seconds"),
                "mensagem": (
                    f"Parabéns! Você concluiu {cursos} cursos e agora é PREMIUM: "
                    "já pode resgatar o voucher e as moedas."
                ),
            }
            self._por_evento[evento_id] = notificacao
        log.info("NOTIFICAÇÃO ENVIADA (%s) ao aluno %s: %s", canal, notificacao["alunoId"], notificacao["mensagem"])
        return True

    def listar(self) -> list[dict]:
        with self._trava:
            return list(self._por_evento.values())