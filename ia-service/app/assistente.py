"""Assistente educacional (RF04) — RAG SIMULADO.

Segue a estrutura de um RAG (Retrieval-Augmented Generation):
  1. recuperação: busca os trechos da base de conhecimento mais parecidos com a pergunta;
  2. geração: monta a resposta a partir dos trechos recuperados, citando as fontes.

A "geração" aqui é extrativa (junta os trechos) para não depender de chave de API.
Para usar um LLM de verdade, basta trocar `gerar_resposta` por uma chamada ao modelo,
passando os trechos recuperados como contexto. O contrato HTTP não muda.
"""
import re
import unicodedata
from dataclasses import dataclass

MODELO_VERSAO = "rag-simulado-v1"


@dataclass(frozen=True)
class Documento:
    id: str
    titulo: str
    texto: str


BASE_DE_CONHECIMENTO: tuple[Documento, ...] = (
    Documento(
        "regra-premium", "Plano PREMIUM",
        "Ao concluir 12 cursos, o aluno passa automaticamente do plano BASICO para o PREMIUM "
        "e recebe um voucher de participação em projetos reais.",
    ),
    Documento(
        "regra-cursos-extras", "Cursos extras",
        "Alunos do plano BASICO que concluem um curso com média maior ou igual a 7,0 "
        "ganham acesso a 3 cursos extras, sem depender de aprovação manual.",
    ),
    Documento(
        "regra-notificacao", "Notificação de mudança de plano",
        "Quando o plano muda para PREMIUM, o aluno é notificado e já pode resgatar "
        "os vouchers e as moedas.",
    ),
    Documento(
        "regra-voucher", "Voucher",
        "O voucher é concedido junto com o plano PREMIUM e dá direito a participar de "
        "projetos reais com empresas parceiras.",
    ),
    Documento(
        "regra-ranking", "Ranking",
        "O ranking ordena os alunos pela quantidade de cursos concluídos; em caso de empate, "
        "aparece primeiro quem se cadastrou antes.",
    ),
)

_STOPWORDS = {
    "a", "o", "as", "os", "de", "da", "do", "das", "dos", "e", "em", "no", "na", "nos", "nas",
    "um", "uma", "para", "pra", "por", "com", "que", "como", "qual", "quais", "quando", "onde",
    "eu", "meu", "minha", "ser", "ter", "tem", "sao", "esta", "isso", "mais", "sem", "ao", "aos",
    "se", "ja", "ou", "the", "faco", "fazer", "posso", "preciso", "quero",
}


def _normalizar(texto: str) -> str:
    sem_acento = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode("ascii")
    return sem_acento.lower()


def _tokens(texto: str) -> set[str]:
    palavras = re.findall(r"[a-z0-9]+", _normalizar(texto))
    tokens = set()
    for p in palavras:
        if p in _STOPWORDS or len(p) < 3:
            continue
        # "radical" ingênuo: plural simples (cursos -> curso, vouchers -> voucher)
        tokens.add(p[:-1] if p.endswith("s") and len(p) > 4 else p)
    return tokens


def recuperar(pergunta: str, k: int = 2) -> list[Documento]:
    """Etapa de recuperação: os k documentos com mais termos em comum com a pergunta.
    Termos que aparecem no título valem mais (o título resume o assunto do trecho)."""
    termos = _tokens(pergunta)
    pontuados = []
    for ordem, doc in enumerate(BASE_DE_CONHECIMENTO):
        pontos = len(termos & _tokens(doc.texto)) + 2 * len(termos & _tokens(doc.titulo))
        if pontos > 0:
            pontuados.append((-pontos, ordem, doc))
    pontuados.sort()
    return [doc for _, _, doc in pontuados[:k]]


def gerar_resposta(pergunta: str, documentos: list[Documento], plano: str | None) -> str:
    """Etapa de geração (simulada): resposta extrativa a partir dos trechos recuperados."""
    if not documentos:
        return (
            "Não encontrei essa informação na base de conhecimento da plataforma. "
            "Tente perguntar sobre plano PREMIUM, voucher, cursos extras ou ranking."
        )
    resposta = " ".join(doc.texto for doc in documentos)
    fala_de_premium = any(doc.id in ("regra-premium", "regra-voucher") for doc in documentos)
    if plano == "BASICO" and fala_de_premium:
        resposta += " Você ainda está no plano BASICO: continue concluindo cursos para chegar ao PREMIUM."
    return resposta


def responder(pergunta: str, plano: str | None = None) -> dict:
    documentos = recuperar(pergunta)
    return {
        "resposta": gerar_resposta(pergunta, documentos, plano),
        "fontes": [doc.id for doc in documentos],
        "modeloVersao": MODELO_VERSAO,
    }