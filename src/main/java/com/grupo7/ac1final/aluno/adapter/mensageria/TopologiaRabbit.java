package com.grupo7.ac1final.aluno.adapter.mensageria;

public final class TopologiaRabbit {

    public static final String EXCHANGE = "gamificacao.eventos";
    public static final String FILA_NOTIFICACAO = "notificacao.aluno-promovido";
    public static final String ROTA_ALUNO_PROMOVIDO = "aluno.promovido-premium";

    private TopologiaRabbit() {
    }
}