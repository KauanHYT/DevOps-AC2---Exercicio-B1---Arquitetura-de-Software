package com.grupo7.ac1final.aluno.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlunoTest {

    @Test
    void deveAlterarPlanoParaPremiumAoConcluir12Cursos () {

        Aluno aluno = new Aluno();

        for(int i = 0; i < 12; i++) {
            aluno.concluirCurso();
        }

        assertEquals("PREMIUM", aluno.getPlano());
    }

    @Test
    void deveReceberVoucherAoConcluir12Cursos() {

        Aluno aluno = new Aluno();

        for (int i = 0; i < 12; i++) {
            aluno.concluirCurso();
        }

        assertTrue(aluno.isPossuiVoucher());
    }

    @Test
    void deveGanharDireitoAMaisTresCursosQuandoAlunoConcluirCursoComMediaMaiorOuIgualA7(){
        Aluno aluno = new Aluno();
        double mediaAluno = 7.0;

        aluno.concluirCurso(mediaAluno);

        assertEquals(3, aluno.getCursosAdicionaisPermitidos());
    }

    @Test
    void NAOdeveGanharDireitoAMaisTresCursosQuandoAlunoConcluirCursoComMediaMaiorOuIgualA7(){
        Aluno aluno = new Aluno();
        double mediaAluno = 6.9;

        aluno.concluirCurso(mediaAluno);

        assertEquals(0, aluno.getCursosAdicionaisPermitidos());
    }

    @Test
    void deveLiberarTresCursosExtrasParaAlunoDoPlanoBasicoComMediaMaiorOuIgualA7() {
        Aluno aluno = new Aluno();
        double mediaAluno = 7.0;

        assertEquals("BASICO", aluno.getPlano());

        aluno.concluirCurso(mediaAluno);

        assertEquals(3, aluno.getCursosAdicionaisPermitidos());
    }

    @Test
    void deveReceberNotificacaoDeMelhoriaDePlanoQuandoAplicacaoIdentificarMudancaParaPremium() {

        Aluno aluno = new Aluno();
        for (int i = 0; i < 12; i++) {
            aluno.concluirCurso();
        }

        aluno.identificarMudancaDePlano();

        assertTrue(aluno.isNotificadoMelhoriaPlano());
    }

    @Test
    void NAOdeveReceberNotificacaoDeMelhoriaDePlanoQuandoAlunoNaoForPremium() {

        Aluno aluno = new Aluno();

        aluno.identificarMudancaDePlano();

        assertFalse(aluno.isNotificadoMelhoriaPlano());
    }
}
