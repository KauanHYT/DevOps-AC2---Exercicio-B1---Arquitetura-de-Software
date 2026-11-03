from app import assistente


def test_recupera_a_regra_certa_e_cita_a_fonte():
    r = assistente.responder("Como eu ganho o voucher?", plano="PREMIUM")

    assert "regra-voucher" in r["fontes"]
    assert "voucher" in r["resposta"].lower()
    assert r["modeloVersao"] == assistente.MODELO_VERSAO


def test_entende_pergunta_sem_acento_e_no_plural():
    r = assistente.responder("quantos cursos extras eu libero com media 7?")

    assert r["fontes"][0] == "regra-cursos-extras"


def test_personaliza_pelo_plano_do_aluno():
    r = assistente.responder("o que preciso para virar premium?", plano="BASICO")

    assert "plano BASICO" in r["resposta"]


def test_pergunta_fora_da_base_nao_inventa_resposta():
    r = assistente.responder("qual a capital da França?")

    assert r["fontes"] == []
    assert "Não encontrei" in r["resposta"]


def test_recupera_no_maximo_dois_trechos():
    assert len(assistente.recuperar("plano premium voucher notificação ranking cursos")) == 2