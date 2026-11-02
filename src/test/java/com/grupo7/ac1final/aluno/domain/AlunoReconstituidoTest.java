package com.grupo7.ac1final.aluno.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As regras continuam valendo para um aluno recriado a partir do estado salvo. */
class AlunoReconstituidoTest {

    @Test
    void alunoReconstituidoCom11CursosViraPremiumNoProximoCurso() {
        Aluno aluno = Aluno.reconstituir(10L, "BASICO", 11, 0, false, false);

        aluno.concluirCurso();

        assertEquals(10L, aluno.getId());
        assertEquals(12, aluno.getCursosConcluidos());
        assertEquals("PREMIUM", aluno.getPlano());
        assertTrue(aluno.isPossuiVoucher());
    }

    @Test
    void reconstituirNaoReaplicaRegras() {
        Aluno aluno = Aluno.reconstituir(1L, "BASICO", 15, 3, false, false);

        assertEquals("BASICO", aluno.getPlano());
        assertEquals(3, aluno.getCursosAdicionaisPermitidos());
    }
}
