"""Cliente HTTP do Core: entrega o evento já traduzido em POST /iot/eventos."""
import json
import logging
import urllib.error
import urllib.request
from enum import Enum
from typing import Callable

log = logging.getLogger("core")


class Resultado(Enum):
    REGISTRADO = "REGISTRADO"      # 201: evento novo
    DUPLICADO = "DUPLICADO"        # 200: retransmissão, o Core já tinha (nada muda)
    RECUSADO = "RECUSADO"          # 400/404: inválido ou aluno inexistente; não adianta repetir
    INDISPONIVEL = "INDISPONIVEL"  # Core fora ou erro 5xx: tentar de novo


def enviar_ao_core(core_url: str, timeout_s: float = 5.0) -> Callable[[dict], Resultado]:
    def enviar(evento: dict) -> Resultado:
        requisicao = urllib.request.Request(
            f"{core_url}/iot/eventos",
            data=json.dumps(evento).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(requisicao, timeout=timeout_s) as resposta:
                return Resultado.REGISTRADO if resposta.status == 201 else Resultado.DUPLICADO
        except urllib.error.HTTPError as e:
            if e.code in (400, 404):
                return Resultado.RECUSADO
            log.warning("Core respondeu %s para o evento %s", e.code, evento.get("eventoId"))
            return Resultado.INDISPONIVEL
        except (urllib.error.URLError, OSError) as e:
            log.warning("Core indisponível para o evento %s: %s", evento.get("eventoId"), e)
            return Resultado.INDISPONIVEL
    return enviar
