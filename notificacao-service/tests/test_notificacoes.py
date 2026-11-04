from app.notificacoes import RegistroNotificacoes


def test_envia_e_registra_a_notificacao():
    registro = RegistroNotificacoes()

    assert registro.enviar({"eventoId": "e1", "alunoId": 7, "cursosConcluidos": 12}, canal="fila") is True

    [n] = registro.listar()
    assert n["alunoId"] == 7
    assert n["canal"] == "fila"
    assert "PREMIUM" in n["mensagem"]


def test_mesmo_evento_duas_vezes_notifica_uma_vez_so():
    registro = RegistroNotificacoes()
    evento = {"eventoId": "e1", "alunoId": 7}

    assert registro.enviar(evento, canal="fila") is True
    assert registro.enviar(evento, canal="fila") is False
    assert len(registro.listar()) == 1


def test_eventos_diferentes_do_mesmo_aluno_sao_notificados():
    registro = RegistroNotificacoes()

    registro.enviar({"eventoId": "e1", "alunoId": 7}, canal="fila")
    registro.enviar({"eventoId": "e2", "alunoId": 7}, canal="http")

    assert len(registro.listar()) == 2