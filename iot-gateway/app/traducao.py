"""Tradução do formato do dispositivo para o formato da plataforma (ADR-005).

Dispositivo (pouca banda e pouca bateria, então tudo é curto):
  tópico   labs/{laboratorio}/dispositivos/{dispositivo}/eventos
  payload  {"id": "<uuid sem hífens>", "a": 7, "t": "C", "ts": 1793767355}
           t: P = presença, I = início de atividade, C = conclusão de atividade

Plataforma (contrato do POST /iot/eventos no Core):
  {"eventoId", "laboratorio", "dispositivoId", "alunoId", "tipo", "ocorridoEm"}

Um aparelho de outro fabricante, com outro formato, ganha outro tradutor aqui; o Core não muda.
"""
import json
from datetime import datetime, timezone

TIPOS = {"P": "PRESENCA", "I": "INICIO_ATIVIDADE", "C": "CONCLUSAO_ATIVIDADE"}


class MensagemInvalida(ValueError):
    """Mensagem que não adianta tentar de novo: está fora do padrão."""


def traduzir(topico: str, payload: bytes) -> dict:
    partes = topico.split("/")
    if len(partes) != 5 or partes[0] != "labs" or partes[2] != "dispositivos" or partes[4] != "eventos":
        raise MensagemInvalida(f"tópico fora do padrão: {topico}")
    try:
        bruto = json.loads(payload.decode("utf-8"))
        evento_id = str(bruto["id"])
        aluno_id = int(bruto["a"])
        tipo = TIPOS[bruto["t"]]
        ocorrido_em = datetime.fromtimestamp(int(bruto["ts"]), tz=timezone.utc)
    except (ValueError, KeyError, TypeError, OverflowError, OSError) as e:
        raise MensagemInvalida(f"payload inválido ({e!r}): {payload[:120]!r}") from e
    if not evento_id or aluno_id <= 0:
        raise MensagemInvalida(f"id ou aluno inválido: {payload[:120]!r}")
    return {
        "eventoId": evento_id,
        "laboratorio": partes[1],
        "dispositivoId": partes[3],
        "alunoId": aluno_id,
        "tipo": tipo,
        "ocorridoEm": ocorrido_em.strftime("%Y-%m-%dT%H:%M:%SZ"),
    }
