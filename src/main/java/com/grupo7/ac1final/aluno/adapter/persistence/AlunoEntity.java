package com.grupo7.ac1final.aluno.adapter.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "alunos")
public class AlunoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plano;

    private boolean possuiVoucher;

    private int cursosConcluidos;

    private int cursosAdicionaisPermitidos;

    private boolean notificadoMelhoriaPlano;

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