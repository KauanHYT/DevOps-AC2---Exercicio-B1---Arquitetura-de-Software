"""API HTTP do serviço de IA (ADR-003).

Contrato consumido pelo Core (Java/Spring Boot):
  POST /recomendacoes          -> RF03 recomendação (ML, simulado por regras)
  POST /assistente/perguntas   -> RF04 assistente (RAG, simulado)
  GET  /health                 -> estado e versão dos modelos

ML e RAG ficam no mesmo serviço, mas em routers e módulos separados,
para que possam virar serviços distintos sem mudar o contrato (ADR-003).

Variável de ambiente para o mini-lab:
  ATRASO_MS  latência artificial (ms) em cada resposta, para simular um modelo lento.
"""
import asyncio
import os

from fastapi import APIRouter, FastAPI
from pydantic import BaseModel, Field

from . import assistente, recomendacao


def atraso_ms() -> int:
    try:
        return max(0, int(os.getenv("ATRASO_MS", "0")))
    except ValueError:
        return 0


async def simular_latencia() -> None:
    atraso = atraso_ms()
    if atraso:
        await asyncio.sleep(atraso / 1000)


# ---------- contrato ----------

class PedidoRecomendacao(BaseModel):
    alunoId: int
    plano: str = "BASICO"
    cursosConcluidos: int = Field(default=0, ge=0)
    limite: int = Field(default=3, ge=1, le=10)


class ItemRecomendado(BaseModel):
    cursoId: str
    titulo: str
    motivo: str


class RespostaRecomendacao(BaseModel):
    alunoId: int
    modeloVersao: str
    itens: list[ItemRecomendado]


class Pergunta(BaseModel):
    alunoId: int
    plano: str = "BASICO"
    pergunta: str = Field(min_length=1, max_length=500)


class RespostaAssistente(BaseModel):
    resposta: str
    fontes: list[str]
    modeloVersao: str


# ---------- RF03: recomendação ----------

router_recomendacao = APIRouter(tags=["recomendação (ML)"])


@router_recomendacao.post("/recomendacoes", response_model=RespostaRecomendacao)
async def recomendar(pedido: PedidoRecomendacao) -> dict:
    await simular_latencia()
    itens = recomendacao.recomendar(pedido.alunoId, pedido.plano, pedido.cursosConcluidos, pedido.limite)
    return {"alunoId": pedido.alunoId, "modeloVersao": recomendacao.MODELO_VERSAO, "itens": itens}


# ---------- RF04: assistente ----------

router_assistente = APIRouter(tags=["assistente (RAG)"])


@router_assistente.post("/assistente/perguntas", response_model=RespostaAssistente)
async def perguntar(pergunta: Pergunta) -> dict:
    await simular_latencia()
    return assistente.responder(pergunta.pergunta, pergunta.plano)


# ---------- aplicação ----------

app = FastAPI(
    title="ia-service",
    version="1.0.0",
    description="Serviço de IA da plataforma (B1 – Desafio 3): recomendação por regras e assistente RAG simulado.",
)
app.include_router(router_recomendacao)
app.include_router(router_assistente)


@app.get("/health", tags=["operação"])
def health() -> dict:
    return {
        "status": "ok",
        "modelos": {
            "recomendacao": recomendacao.MODELO_VERSAO,
            "assistente": assistente.MODELO_VERSAO,
        },
        "atrasoMs": atraso_ms(),
    }