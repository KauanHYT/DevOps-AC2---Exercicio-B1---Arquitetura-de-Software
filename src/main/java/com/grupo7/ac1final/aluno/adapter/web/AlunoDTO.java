package com.grupo7.ac1final.aluno.adapter.web;

import com.grupo7.ac1final.aluno.domain.Aluno;

public class AlunoDTO {

    private Long id;
    private String plano;
    private boolean possuiVoucher;
    private int cursosConcluidos;
    private int cursosAdicionaisPermitidos;
    private boolean notificadoMelhoriaPlano;

    /** Tradução domínio -&gt; contrato HTTP; o JSON da API continua igual ao da AC1. */
    public static AlunoDTO de(Aluno aluno) {
        AlunoDTO dto = new AlunoDTO();
        dto.setId(aluno.getId());
        dto.setPlano(aluno.getPlano());
        dto.setPossuiVoucher(aluno.isPossuiVoucher());
        dto.setCursosConcluidos(aluno.getCursosConcluidos());
        dto.setCursosAdicionaisPermitidos(aluno.getCursosAdicionaisPermitidos());
        dto.setNotificadoMelhoriaPlano(aluno.isNotificadoMelhoriaPlano());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public boolean isPossuiVoucher() {
        return possuiVoucher;
    }

    public void setPossuiVoucher(boolean possuiVoucher) {
        this.possuiVoucher = possuiVoucher;
    }

    public int getCursosConcluidos() {
        return cursosConcluidos;
    }

    public void setCursosConcluidos(int cursosConcluidos) {
        this.cursosConcluidos = cursosConcluidos;
    }

    public int getCursosAdicionaisPermitidos() {
        return cursosAdicionaisPermitidos;
    }

    public void setCursosAdicionaisPermitidos(int cursosAdicionaisPermitidos) {
        this.cursosAdicionaisPermitidos = cursosAdicionaisPermitidos;
    }

    public boolean isNotificadoMelhoriaPlano() {
        return notificadoMelhoriaPlano;
    }

    public void setNotificadoMelhoriaPlano(boolean notificadoMelhoriaPlano) {
        this.notificadoMelhoriaPlano = notificadoMelhoriaPlano;
    }
}
