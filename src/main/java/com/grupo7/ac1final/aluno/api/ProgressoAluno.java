package com.grupo7.ac1final.aluno.api;

/**
 * Visão pública e somente leitura do progresso de um aluno.
 * É o único tipo do módulo aluno que outros módulos (ex.: ranking) podem enxergar.
 */
public record ProgressoAluno(Long alunoId, String plano, int cursosConcluidos) {
}
