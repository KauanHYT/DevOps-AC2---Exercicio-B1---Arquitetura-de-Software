package com.grupo7.ac1final.aluno.application;

import com.grupo7.ac1final.aluno.adapter.memoria.AlunoRepositoryEmMemoria;
import com.grupo7.ac1final.aluno.application.port.NotificacaoIndisponivelException;
import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;
import com.grupo7.ac1final.aluno.domain.Aluno;
import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromocaoPremiumTest {

    private final AlunoRepositoryEmMemoria repositorio = new AlunoRepositoryEmMemoria();
    private final List<AlunoPromovidoPremium> eventos = new ArrayList<>();

    private AlunoService servicoQueResponde(NotificadorPromocao.Resultado resultado) {
        return new AlunoService(repositorio, evento -> {
            eventos.add(evento);
            return resultado;
        });
    }

    private Long novoAluno(AlunoService service) {
        return service.cadastrar(new DadosCadastroAluno("BASICO", 0, 0, false, false)).getId();
    }

    @Test
    void promocaoGeraExatamenteUmEventoNoDecimoSegundoCurso() {
        AlunoService service = servicoQueResponde(NotificadorPromocao.Resultado.AGENDADA);
        Long id = novoAluno(service);

        for (int i = 0; i < 11; i++) {
            service.concluirCurso(id);
        }
        assertTrue(eventos.isEmpty());

        service.concluirCurso(id);
        service.concluirCurso(id);

        assertEquals(1, eventos.size());
        assertEquals(id, eventos.get(0).alunoId());
        assertEquals(12, eventos.get(0).cursosConcluidos());
    }

    @Test
    void modoAssincronoConcluiEPromoveNaHoraMasSoMarcaNotificadoDepois() {
        AlunoService service = servicoQueResponde(NotificadorPromocao.Resultado.AGENDADA);
        Long id = novoAluno(service);

        Aluno aluno = null;
        for (int i = 0; i < 12; i++) {
            aluno = service.concluirCurso(id).orElseThrow();
        }

        assertEquals("PREMIUM", aluno.getPlano());
        assertTrue(aluno.isPossuiVoucher());
        assertFalse(aluno.isNotificadoMelhoriaPlano(), "quem marca é o consumidor, quando entregar");
    }

    @Test
    void modoSincronoMarcaNotificadoNaMesmaOperacao() {
        AlunoService service = servicoQueResponde(NotificadorPromocao.Resultado.ENTREGUE);
        Long id = novoAluno(service);

        Aluno aluno = null;
        for (int i = 0; i < 12; i++) {
            aluno = service.concluirCurso(id).orElseThrow();
        }

        assertTrue(aluno.isNotificadoMelhoriaPlano());
        assertTrue(repositorio.buscarPorId(id).orElseThrow().isNotificadoMelhoriaPlano());
    }

    @Test
    void alunoQueJaEraPremiumNaoGeraNovoEvento() {
        AlunoService service = servicoQueResponde(NotificadorPromocao.Resultado.AGENDADA);
        Long id = service.cadastrar(new DadosCadastroAluno("PREMIUM", 12, 0, true, true)).getId();

        service.concluirCurso(id);

        assertTrue(eventos.isEmpty());
    }

    @Test
    void falhaDoNotificadorSincronoPropagaParaQueATransacaoSejaDesfeita() {
        AlunoService service = new AlunoService(repositorio, evento -> {
            throw new NotificacaoIndisponivelException("connection refused", null);
        });
        Long id = novoAluno(service);
        for (int i = 0; i < 11; i++) {
            service.concluirCurso(id);
        }

        boolean propagou = false;
        try {
            service.concluirCurso(id);
        } catch (NotificacaoIndisponivelException e) {
            propagou = true;
        }
        assertTrue(propagou);
    }
}