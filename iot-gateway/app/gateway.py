"""Decisão por mensagem do gateway (ADR-005), separada do MQTT para ser testável.

Para cada mensagem: traduz, entrega ao Core e só então volta. Como o paho confirma
(PUBACK) a mensagem QoS 1 depois que esta função retorna, uma mensagem nunca é
confirmada antes de chegar ao Core. Com o Core fora, o gateway espera e tenta de novo;
se o próprio gateway cair no meio, o broker reentrega, e o Core descarta a repetição
pelo eventoId.
"""
import logging
import time
from dataclasses import dataclass
from typing import Callable

from .core import Resultado
from .traducao import MensagemInvalida, traduzir

log = logging.getLogger("gateway")


@dataclass
class Contadores:
    recebidas: int = 0
    registradas: int = 0
    duplicadas: int = 0
    recusadas: int = 0
    invalidas: int = 0


def processar(topico: str, payload: bytes, enviar: Callable[[dict], Resultado],
              contadores: Contadores, esperar: Callable[[float], None] = time.sleep,
              max_tentativas: int | None = None) -> str:
    contadores.recebidas += 1
    try:
        evento = traduzir(topico, payload)
    except MensagemInvalida as e:
        contadores.invalidas += 1
        log.error("mensagem descartada: %s", e)
        return "DESCARTADA"

    espera = 1.0
    tentativas = 0
    while True:
        resultado = enviar(evento)
        if resultado is not Resultado.INDISPONIVEL:
            break
        tentativas += 1
        if max_tentativas is not None and tentativas >= max_tentativas:
            return Resultado.INDISPONIVEL.name
        log.warning("Core indisponível; nova tentativa do evento %s em %.0f s", evento["eventoId"], espera)
        esperar(espera)
        espera = min(espera * 2, 10.0)

    if resultado is Resultado.REGISTRADO:
        contadores.registradas += 1
    elif resultado is Resultado.DUPLICADO:
        contadores.duplicadas += 1
    else:
        contadores.recusadas += 1
    log.info("evento %s (%s, aluno %s, %s/%s) -> %s", evento["eventoId"], evento["tipo"], evento["alunoId"],
             evento["laboratorio"], evento["dispositivoId"], resultado.name)
    return resultado.name
