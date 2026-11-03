"""Recomendação inteligente (RF03) — modelo baseado em regras, SIMULADO.

O Desafio 3 avalia a fronteira arquitetural entre o Core (Java) e a IA (Python),
não a qualidade do modelo. Por isso o "modelo" é um conjunto de regras
determinísticas. Ele pode ser trocado por um modelo treinado (scikit-learn, etc.)
sem alterar o contrato HTTP, que é o que o Core conhece.
"""
from dataclasses import dataclass

MODELO_VERSAO = "regras-v1"


@dataclass(frozen=True)
class Curso:
    id: str
    titulo: str
    nivel: int
    trilha: str


CATALOGO: tuple[Curso, ...] = (
    Curso("FUND-101", "Fundamentos de Programação", 1, "fundamentos"),
    Curso("FUND-102", "Lógica e Algoritmos", 1, "fundamentos"),
    Curso("WEB-110", "HTML, CSS e JavaScript", 1, "web"),
    Curso("DADOS-120", "SQL para Iniciantes", 1, "dados"),
    Curso("WEB-210", "APIs REST com Spring Boot", 2, "web"),
    Curso("DADOS-220", "Análise de Dados com Python", 2, "dados"),
    Curso("DEVOPS-230", "Docker e Integração Contínua", 2, "devops"),
    Curso("ARQ-310", "Arquitetura de Software na Prática", 3, "arquitetura"),
    Curso("IA-320", "Machine Learning Aplicado", 3, "dados"),
    Curso("DEVOPS-330", "Observabilidade com Prometheus e Grafana", 3, "devops"),
    Curso("PROJ-400", "Projeto Real com Empresa Parceira", 3, "projetos"),
)

CURSO_EXCLUSIVO_PREMIUM = "PROJ-400"

_MOTIVO_POR_NIVEL = {
    1: "Base para quem está começando (0 a 3 cursos concluídos).",
    2: "Próximo passo para quem já concluiu de 4 a 8 cursos.",
    3: "Aprofundamento para quem já concluiu 9 ou mais cursos.",
}


def nivel_para(cursos_concluidos: int) -> int:
    if cursos_concluidos < 4:
        return 1
    if cursos_concluidos < 9:
        return 2
    return 3


def recomendar(aluno_id: int, plano: str, cursos_concluidos: int, limite: int = 3) -> list[dict]:
    """Devolve até `limite` cursos adequados ao progresso do aluno.

    - nível pelo número de cursos concluídos;
    - o curso de projeto real só aparece para PREMIUM (e vem primeiro);
    - "personalização" determinística: a lista é rotacionada pelo id do aluno.
    """
    premium = plano == "PREMIUM"
    nivel = nivel_para(cursos_concluidos)
    candidatos = [
        c for c in CATALOGO
        if c.nivel == nivel and (c.id != CURSO_EXCLUSIVO_PREMIUM or premium)
    ]
    if not candidatos:
        return []

    deslocamento = aluno_id % len(candidatos)
    ordenados = candidatos[deslocamento:] + candidatos[:deslocamento]
    if premium:
        ordenados.sort(key=lambda c: c.id != CURSO_EXCLUSIVO_PREMIUM)

    return [
        {
            "cursoId": c.id,
            "titulo": c.titulo,
            "motivo": (
                "Exclusivo PREMIUM: use o seu voucher em um projeto real."
                if c.id == CURSO_EXCLUSIVO_PREMIUM
                else _MOTIVO_POR_NIVEL[nivel]
            ),
        }
        for c in ordenados[:limite]
    ]