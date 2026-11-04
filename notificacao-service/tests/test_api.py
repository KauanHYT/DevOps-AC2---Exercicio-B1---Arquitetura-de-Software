"""Testes da API HTTP. Precisam de fastapi + httpx (requirements-dev.txt)."""
import os

import pytest

pytest.importorskip("fastapi")
pytest.importorskip("httpx")

os.environ["CONSUMIDOR_HABILITADO"] = "false"  # sem RabbitMQ nos testes

from fastapi.testclient import TestClient  # noqa: E402

from app.main import app  # noqa: E402


def test_canal_sincrono_registra_e_e_idempotente():
    with TestClient(app) as cliente:
        evento = {"eventoId": "api-1", "alunoId": 5, "cursosConcluidos": 12}

        primeira = cliente.post("/notificacoes", json=evento)
        segunda = cliente.post("/notificacoes", json=evento)

        assert primeira.status_code == 201
        assert segunda.status_code == 200
        assert segunda.json()["duplicada"] is True
        assert [n["eventoId"] for n in cliente.get("/notificacoes").json()].count("api-1") == 1


def test_health():
    with TestClient(app) as cliente:
        corpo = cliente.get("/health").json()

        assert corpo["status"] == "ok"
        assert corpo["consumidorConectado"] is False