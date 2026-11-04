from app.simulador import montar_eventos, payload


def test_cada_evento_tem_id_proprio_e_o_horario_em_que_aconteceu():
    eventos = montar_eventos(aluno=7, presenca=True, inicio=True, conclusoes=2, agora=1000)

    assert [e["t"] for e in eventos] == ["P", "I", "C", "C"]
    assert len({e["id"] for e in eventos}) == 4
    assert [e["ts"] for e in eventos] == [1000, 1001, 1002, 1003]


def test_payload_compacto_para_dispositivo_com_pouca_banda():
    [evento] = montar_eventos(aluno=7, presenca=False, inicio=False, conclusoes=1, agora=1793767355)

    assert len(payload(evento)) < 80
