package com.grupo7.ac1final.aluno.application;

import com.grupo7.ac1final.aluno.adapter.memoria.AlunoRepositoryEmMemoria;
import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.domain.Aluno;
import org.junit.jupiter.api.Test;
import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlunoServiceTest {

    private final AlunoRepositoryEmMemoria repositorio = new AlunoRepositoryEmMemoria();
    private final AlunoService service = new AlunoService(repositorio, evento -> NotificadorPromocao.Resultado.AGENDADA);

    private Long novoAluno() {
        return service.cadastrar(new DadosCadastroAluno("BASICO", 0, 0, false, false)).getId();
    }

    @Test
    void concluirDozeCursosPeloCasoDeUsoTornaAlunoPremiumComVoucher() {
        Long id = novoAluno();

        for (int i = 0; i < 12; i++) {
            service.concluirCurso(id);
        }

        Aluno aluno = repositorio.buscarPorId(id).orElseThrow();
        assertEquals("PREMIUM", aluno.getPlano());
        assertTrue(aluno.isPossuiVoucher());
    }

    @Test
    void liberarCursosComMediaSeteLiberaTresCursos() {
        Long id = novoAluno();

        Aluno aluno = service.liberarCursosExtras(id, 7.0).orElseThrow();

        assertEquals(3, aluno.getCursosAdicionaisPermitidos());
    }

    @Test
    void liberarCursosComMediaAbaixoDeSeteNaoLiberaCursos() {
        Long id = novoAluno();

        Aluno aluno = service.liberarCursosExtras(id, 6.9).orElseThrow();

        assertEquals(0, aluno.getCursosAdicionaisPermitidos());
    }

    @Test
    void notificacaoSoERegistradaParaAlunoPremium() {
        Long basico = novoAluno();
        Long premium = service.cadastrar(new DadosCadastroAluno("PREMIUM", 12, 0, true, false)).getId();

        assertFalse(service.registrarNotificacaoMudancaPlano(basico).orElseThrow().isNotificadoMelhoriaPlano());
        assertTrue(service.registrarNotificacaoMudancaPlano(premium).orElseThrow().isNotificadoMelhoriaPlano());
    }

    @Test
    void cadastroSemPlanoAssumeBasico() {
        Aluno aluno = service.cadastrar(new DadosCadastroAluno(null, 0, 0, false, false));

        assertEquals("BASICO", aluno.getPlano());
    }

    @Test
    void operacoesEmAlunoInexistenteRetornamVazio() {
        assertTrue(service.concluirCurso(42L).isEmpty());
        assertTrue(service.liberarCursosExtras(42L, 9.0).isEmpty());
        assertTrue(service.registrarNotificacaoMudancaPlano(42L).isEmpty());
        assertFalse(service.remover(42L));
    }

    @Test
    void apiPublicaDoModuloEntregaProgressoOrdenadoParaORanking() {
        Long a = novoAluno();
        Long b = novoAluno();
        service.concluirCurso(b);

        List<ProgressoAluno> progresso = service.listarMaioresProgressos(10);

        assertEquals(new ProgressoAluno(b, "BASICO", 1), progresso.get(0));
        assertEquals(new ProgressoAluno(a, "BASICO", 0), progresso.get(1));
    }
}
