package com.grupo7.ac1final.iot.domain;

/**
 * Tipos de evento que os dispositivos dos laboratórios produzem (RF06).
 * Só a conclusão de atividade altera o progresso do aluno; presença e início
 * ficam registrados para o ecossistema (analytics, frequência).
 */
public enum TipoEventoIot {
    PRESENCA(false),
    INICIO_ATIVIDADE(false),
    CONCLUSAO_ATIVIDADE(true);

    private final boolean concluiCurso;

    TipoEventoIot(boolean concluiCurso) {
        this.concluiCurso = concluiCurso;
    }

    public boolean concluiCurso() {
        return concluiCurso;
    }
}