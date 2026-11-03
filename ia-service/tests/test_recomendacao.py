from app import recomendacao


def test_iniciante_recebe_cursos_de_nivel_1():
    itens = recomendacao.recomendar(aluno_id=1, plano="BASICO", cursos_concluidos=0, limite=3)

    assert len(itens) == 3
    ids = {i["cursoId"] for i in itens}
    nivel_1 = {c.id for c in recomendacao.CATALOGO if c.nivel == 1}
    assert ids <= nivel_1


def test_nivel_acompanha_o_progresso():
    assert recomendacao.nivel_para(3) == 1
    assert recomendacao.nivel_para(4) == 2
    assert recomendacao.nivel_para(8) == 2
    assert recomendacao.nivel_para(9) == 3


def test_projeto_real_so_aparece_para_premium_e_vem_primeiro():
    basico = recomendacao.recomendar(aluno_id=5, plano="BASICO", cursos_concluidos=12, limite=10)
    premium = recomendacao.recomendar(aluno_id=5, plano="PREMIUM", cursos_concluidos=12, limite=10)

    assert recomendacao.CURSO_EXCLUSIVO_PREMIUM not in {i["cursoId"] for i in basico}
    assert premium[0]["cursoId"] == recomendacao.CURSO_EXCLUSIVO_PREMIUM


def test_recomendacao_e_deterministica_por_aluno():
    a = recomendacao.recomendar(aluno_id=7, plano="BASICO", cursos_concluidos=5)
    b = recomendacao.recomendar(aluno_id=7, plano="BASICO", cursos_concluidos=5)
    outro = recomendacao.recomendar(aluno_id=8, plano="BASICO", cursos_concluidos=5)

    assert a == b
    assert [i["cursoId"] for i in a] != [i["cursoId"] for i in outro]


def test_respeita_o_limite():
    assert len(recomendacao.recomendar(aluno_id=1, plano="BASICO", cursos_concluidos=0, limite=1)) == 1