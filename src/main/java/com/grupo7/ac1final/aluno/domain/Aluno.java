package com.grupo7.ac1final.aluno.domain;

/**
 * Domínio do aluno: concentra as regras de gamificação das US 1 a 4.
 * Classe Java pura, sem Spring nem JPA (ADR-002); a persistência acontece
 * por meio da porta AlunoRepositoryPort, implementada pelos adapters.
 */
public class Aluno {

    private Long id;
    private int cursosAdicionaisPermitidos = 0;
    private int cursosConcluidos = 0;
    private String plano = "BASICO";
    private boolean possuiVoucher = false;
    private boolean notificadoMelhoriaPlano = false;

    public Aluno() {
    }

    /**
     * Recria um aluno a partir de um estado já existente (ex.: lido do banco
     * por um adapter). Não reaplica regras; elas valem a partir das próximas ações.
     */
    public static Aluno reconstituir(Long id, String plano, int cursosConcluidos,
                                     int cursosAdicionaisPermitidos, boolean possuiVoucher,
                                     boolean notificadoMelhoriaPlano) {
        Aluno aluno = new Aluno();
        aluno.id = id;
        aluno.plano = plano;
        aluno.cursosConcluidos = cursosConcluidos;
        aluno.cursosAdicionaisPermitidos = cursosAdicionaisPermitidos;
        aluno.possuiVoucher = possuiVoucher;
        aluno.notificadoMelhoriaPlano = notificadoMelhoriaPlano;
        return aluno;
    }

    public Long getId() {
        return id;
    }

    public int getCursosConcluidos() {
        return cursosConcluidos;
    }

    public void concluirCurso() {
        cursosConcluidos++;

        if (cursosConcluidos >= 12) {
            plano = "PREMIUM";
            possuiVoucher = true;
        }
    }

    public String getPlano() {
        return plano;
    }

    public boolean isPremium() {
        return "PREMIUM".equals(plano);
    }

    public boolean isPossuiVoucher() {
        return possuiVoucher;
    }

    public void concluirCurso(double mediaAluno) {

        if (mediaAluno >= 7.0)
            this.cursosAdicionaisPermitidos = 3;
        else
            this.cursosAdicionaisPermitidos = 0;
    }

    public int getCursosAdicionaisPermitidos() {
        return this.cursosAdicionaisPermitidos;
    }

    public void identificarMudancaDePlano() {
        if ("PREMIUM".equals(plano)) {
            notificadoMelhoriaPlano = true;
        }
    }

    public boolean isNotificadoMelhoriaPlano() {
        return notificadoMelhoriaPlano;
    }
}