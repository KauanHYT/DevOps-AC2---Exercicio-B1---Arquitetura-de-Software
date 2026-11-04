package com.grupo7.ac1final.aluno.domain;

import java.time.Instant;
import java.util.UUID;

public record AlunoPromovidoPremium(String eventoId, Long alunoId, int cursosConcluidos, Instant ocorridoEm) {

    public static AlunoPromovidoPremium de(Aluno aluno, Instant agora) {
        return new AlunoPromovidoPremium(UUID.randomUUID().toString(), aluno.getId(), aluno.getCursosConcluidos(), agora);
    }
}