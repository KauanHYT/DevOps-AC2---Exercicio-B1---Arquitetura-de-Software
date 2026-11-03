"""Testes do contrato HTTP. Precisam de fastapi + httpx (requirements-dev.txt)."""
import pytest

pytest.importorskip("fastapi")
pytest.importorskip("httpx")

from fastapi.testclient import TestClient  # noqa: E402

from app.main import app  # noqa: E402

cliente = TestClient(app)


def test_health_informa_versao_dos_modelos():
    r = cliente.get("/health")

    assert r.status_code == 200
    assert r.json()["modelos"]["recomendacao"] == "regras-v1"


def test_contrato_de_recomendacao():
    r = cliente.post("/recomendacoes", json={"alunoId": 1, "plano": "BASICO", "cursosConcluidos": 0, "limite": 2})

    assert r.status_code == 200
    corpo = r.json()
    assert corpo["alunoId"] == 1
    assert corpo["modeloVersao"] == "regras-v1"
    assert len(corpo["itens"]) == 2
    assert set(corpo["itens"][0]) == {"cursoId", "titulo", "motivo"}


def test_contrato_do_assistente():
    r = cliente.post("/assistente/perguntas", json={"alunoId": 1, "plano": "BASICO", "pergunta": "como ganho voucher?"})

    assert r.status_code == 200
    assert set(r.json()) == {"resposta", "fontes", "modeloVersao"}


def test_valida_entrada():
    assert cliente.post("/recomendacoes", json={"alunoId": 1, "limite": 50}).status_code == 422
    assert cliente.post("/assistente/perguntas", json={"alunoId": 1, "pergunta": ""}).status_code == 422