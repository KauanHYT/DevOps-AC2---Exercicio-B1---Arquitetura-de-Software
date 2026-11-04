"""Contrato HTTP com o Core, contra um servidor falso (http.server da biblioteca padrão)."""
import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

import pytest

from app.core import Resultado, enviar_ao_core

EVENTO = {"eventoId": "e1", "laboratorio": "lab-01", "dispositivoId": "leitor-07", "alunoId": 7,
          "tipo": "CONCLUSAO_ATIVIDADE", "ocorridoEm": "2026-11-04T04:42:35Z"}


@pytest.fixture
def core_falso():
    estado = {"status": 201, "recebidos": []}

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            tamanho = int(self.headers["Content-Length"])
            estado["recebidos"].append((self.path, json.loads(self.rfile.read(tamanho))))
            self.send_response(estado["status"])
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b"{}")

        def log_message(self, *args):
            pass

    servidor = HTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=servidor.serve_forever, daemon=True).start()
    yield f"http://127.0.0.1:{servidor.server_address[1]}", estado
    servidor.shutdown()


@pytest.mark.parametrize("status, esperado", [
    (201, Resultado.REGISTRADO),
    (200, Resultado.DUPLICADO),
    (404, Resultado.RECUSADO),
    (400, Resultado.RECUSADO),
    (503, Resultado.INDISPONIVEL),
])
def test_status_do_core_vira_a_decisao_do_gateway(core_falso, status, esperado):
    url, estado = core_falso
    estado["status"] = status

    assert enviar_ao_core(url)(EVENTO) is esperado
    assert estado["recebidos"] == [("/iot/eventos", EVENTO)]


def test_core_desligado_e_indisponivel():
    assert enviar_ao_core("http://127.0.0.1:9", timeout_s=1)(EVENTO) is Resultado.INDISPONIVEL
