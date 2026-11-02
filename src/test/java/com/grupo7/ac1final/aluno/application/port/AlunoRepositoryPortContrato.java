package com.grupo7.ac1final.aluno.application.port;

import com.grupo7.ac1final.aluno.domain.Aluno;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato da porta AlunoRepositoryPort (evidência do Desafio 2).
 * Os mesmos testes rodam contra os dois adapters, o fake em memória e o JPA:
 * se ambos passam, trocar o adapter não muda o comportamento visto pela aplicação.
 */
public abstract class AlunoRepositoryPortContrato {

    protected abstract AlunoRepositoryPort repositorio();

    @Test
    void deveGerarIdAoSalvarNovoAlunoPreservandoEstadoInicial() {
        Aluno salvo = repositorio().salvar(new Aluno());

        assertNotNull(salvo.getId());
        assertEquals("BASICO", salvo.getPlano());
        assertEquals(0, salvo.getCursosConcluidos());
        assertFalse(salvo.isPossuiVoucher());
    }

    @Test
    void devePersistirRegraDeDominioAplicadaAoAlunoExistente() {
        Aluno aluno = repositorio().salvar(new Aluno());
        for (int i = 0; i < 12; i++) {
            aluno.concluirCurso();
        }
        repositorio().salvar(aluno);

        Aluno relido = repositorio().buscarPorId(aluno.getId()).orElseThrow();
        assertEquals("PREMIUM", relido.getPlano());
        assertEquals(12, relido.getCursosConcluidos());
        assertTrue(relido.isPossuiVoucher());
    }

    @Test
    void deveRetornarVazioParaIdInexistente() {
        assertTrue(repositorio().buscarPorId(999_999L).isEmpty());
        assertFalse(repositorio().existe(999_999L));
    }

    @Test
    void deveRemoverAluno() {
        Aluno aluno = repositorio().salvar(new Aluno());

        repositorio().remover(aluno.getId());

        assertFalse(repositorio().existe(aluno.getId()));
    }

    @Test
    void deveListarMaioresProgressosEmOrdemDecrescenteRespeitandoLimite() {
        Aluno comDois = salvarComCursos(2);
        Aluno primeiroComCinco = salvarComCursos(5);
        Aluno segundoComCinco = salvarComCursos(5);

        List<Aluno> top = repositorio().buscarMaioresProgressos(2);

        assertEquals(2, top.size());
        assertEquals(primeiroComCinco.getId(), top.get(0).getId());
        assertEquals(segundoComCinco.getId(), top.get(1).getId());
        assertFalse(top.stream().anyMatch(a -> a.getId().equals(comDois.getId())));
    }

    private Aluno salvarComCursos(int cursos) {
        Aluno aluno = new Aluno();
        for (int i = 0; i < cursos; i++) {
            aluno.concluirCurso();
        }
        return repositorio().salvar(aluno);
    }
}
