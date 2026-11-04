"""API HTTP do serviço de notificação (ADR-004).

  POST /notificacoes   canal SÍNCRONO: usado só quando o Core roda com NOTIFICACAO_MODO=sincrono
                       (alternativa A do ADR-004, mantida para o mini-lab comparar)
  GET  /notificacoes   notificações já enviadas (evidência do mini-lab)
  GET  /health         estado do serviço e da conexão com o RabbitMQ

O canal ASSÍNCRONO (decisão do ADR-004) é o consumidor da fila, iniciado junto com a API.
"""
import logging
import os
import threading
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .consumidor import consumir_para_sempre
from .notificacoes import RegistroNotificacoes

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s [%(name)s] %(message)s")

registro = RegistroNotificacoes()
estado_consumidor = {"conectado": False}


@asynccontextmanager
async def ciclo_de_vida(_app: FastAPI):
    if os.getenv("CONSUMIDOR_HABILITADO", "true").lower() == "true":
        threading.Thread(
            target=consumir_para_sempre, args=(registro, estado_consumidor),
            name="consumidor-rabbitmq", daemon=True,
        ).start()
    yield


app = FastAPI(
    title="notificacao-service",
    version="1.0.0",
    description="Notificação de promoção para PREMIUM (B1 – Desafio 4).",
    lifespan=ciclo_de_vida,
)


class EventoPromocao(BaseModel):
    eventoId: str
    alunoId: int
    tipo: str = "AlunoPromovidoPremium"
    cursosConcluidos: int = 12
    ocorridoEm: Any = None


@app.post("/notificacoes", status_code=201)
def notificar(evento: EventoPromocao):
    nova = registro.enviar(evento.model_dump(), canal="http")
    corpo = {"eventoId": evento.eventoId, "duplicada": not nova}
    return corpo if nova else JSONResponse(status_code=200, content=corpo)


@app.get("/notificacoes")
def listar() -> list[dict]:
    return registro.listar()


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "consumidorConectado": estado_consumidor["conectado"],
        "notificacoesEnviadas": len(registro.listar()),
    }