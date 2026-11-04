import json

from app.core import Resultado
from app.gateway import Contadores, processar

TOPICO = "labs/lab-01/dispositivos/leitor-07/eventos"
MENSAGEM = json.dumps({"id": "e1", "a": 7, "t": "C", "ts": 1793767355}).encode("utf-8")


def test_evento_novo_e_entregue_ao_core():
    entregues = []
    contadores = Contadores()

    resultado = processar(TOPICO, MENSAGEM, lambda e: entregues.append(e) or Resultado.REGISTRADO, contadores)

    assert resultado == "REGISTRADO"
    assert entregues[0]["eventoId"] == "e1"
    assert contadores.registradas == 1


def test_retransmissao_chega_ao_core_e_volta_como_duplicado():
    contadores = Contadores()

    processar(TOPICO, MENSAGEM, lambda e: Resultado.REGISTRADO, contadores)
    resultado = processar(TOPICO, MENSAGEM, lambda e: Resultado.DUPLICADO, contadores)

    assert resultado == "DUPLICADO"
    assert (contadores.registradas, contadores.duplicadas) == (1, 1)


def test_core_fora_do_ar_o_gateway_espera_e_tenta_de_novo_sem_perder():
    respostas = [Resultado.INDISPONIVEL, Resultado.INDISPONIVEL, Resultado.REGISTRADO]
    esperas = []

    resultado = processar(TOPICO, MENSAGEM, lambda e: respostas.pop(0), Contadores(), esperar=esperas.append)

    assert resultado == "REGISTRADO"
    assert esperas == [1.0, 2.0]


def test_mensagem_invalida_e_descartada_sem_chamar_o_core():
    chamadas = []
    contadores = Contadores()

    resultado = processar(TOPICO, b"lixo", lambda e: chamadas.append(e), contadores)

    assert resultado == "DESCARTADA"
    assert chamadas == []
    assert contadores.invalidas == 1


def test_aluno_inexistente_e_definitivo_e_nao_e_repetido():
    chamadas = []

    def nao_deveria_esperar(_segundos):
        raise AssertionError("RECUSADO não deve ser repetido")

    resultado = processar(TOPICO, MENSAGEM, lambda e: chamadas.append(e) or Resultado.RECUSADO, Contadores(),
                          esperar=nao_deveria_esperar)

    assert resultado == "RECUSADO"
    assert len(chamadas) == 1
