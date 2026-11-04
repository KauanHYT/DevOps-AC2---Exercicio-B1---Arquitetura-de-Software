import json

import pytest

from app.traducao import MensagemInvalida, traduzir

TOPICO = "labs/lab-01/dispositivos/leitor-07/eventos"


def corpo(**campos) -> bytes:
    evento = {"id": "a1b2c3", "a": 7, "t": "C", "ts": 1793767355}
    evento.update(campos)
    return json.dumps(evento).encode("utf-8")


def test_traduz_o_formato_compacto_para_o_da_plataforma():
    assert traduzir(TOPICO, corpo()) == {
        "eventoId": "a1b2c3",
        "laboratorio": "lab-01",
        "dispositivoId": "leitor-07",
        "alunoId": 7,
        "tipo": "CONCLUSAO_ATIVIDADE",
        "ocorridoEm": "2026-11-04T04:42:35Z",
    }


def test_presenca_e_inicio_de_atividade():
    assert traduzir(TOPICO, corpo(t="P"))["tipo"] == "PRESENCA"
    assert traduzir(TOPICO, corpo(t="I"))["tipo"] == "INICIO_ATIVIDADE"


@pytest.mark.parametrize("topico", ["labs/lab-01/eventos", "outro/lab-01/dispositivos/leitor-07/eventos"])
def test_topico_fora_do_padrao_e_recusado(topico):
    with pytest.raises(MensagemInvalida):
        traduzir(topico, corpo())


@pytest.mark.parametrize("payload", [b"nao e json", corpo(t="X"), corpo(a=0), b'{"id":"a1"}'])
def test_payload_invalido_e_recusado(payload):
    with pytest.raises(MensagemInvalida):
        traduzir(TOPICO, payload)
