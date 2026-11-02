package com.grupo7.ac1final.aluno.application;

/** Dados de entrada do caso de uso de cadastro (mantém o comportamento do POST da AC1). */
public record DadosCadastroAluno(String plano, int cursosConcluidos, int cursosAdicionaisPermitidos,
                                 boolean possuiVoucher, boolean notificadoMelhoriaPlano) {
}
