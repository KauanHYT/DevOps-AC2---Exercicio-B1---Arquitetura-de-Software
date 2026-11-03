package com.grupo7.ac1final.ia.domain;

import java.util.List;

/** Recomendações entregues ao aluno, com a origem explícita (IA ou fallback). */
public record Recomendacoes(Long alunoId, OrigemRecomendacao origem, String modeloVersao,
                            List<ItemRecomendado> itens) {
}