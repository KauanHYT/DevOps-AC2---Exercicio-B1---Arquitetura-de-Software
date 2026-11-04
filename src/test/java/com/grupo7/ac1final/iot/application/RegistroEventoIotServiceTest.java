package com.grupo7.ac1final.iot.application;

import com.grupo7.ac1final.aluno.api.ConclusaoDeCurso;
import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.iot.application.RegistroEventoIotService.Resultado;
import com.grupo7.ac1final.iot.application.port.EventoIotRepositoryPort;
import com.grupo7.ac1final.iot.domain.EventoIot;
import com.grupo7.ac1final.iot.domain.TipoEventoIot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caso de uso do RF06 com portas falsas: sem Spring, sem banco, sem MQTT. */
class RegistroEventoIotServiceTest {

    private static final long ALUNO = 7L;
    private static final Instant AGORA = Instant.parse("2026-11-04T12:00:00Z");

    /** Adapter em memória da porta de eventos. */
    static class EventosEmMemoria implements EventoIotRepositoryPort {
        final List<EventoIot> registrados = new ArrayList<>();

        @Override
        public boolean jaRegistrado(String eventoId) {
            return registrados.stream().anyMatch(e -> e.eventoId().equals(eventoId));
        }

        @Override
        public void registrar(EventoIot evento) {
            registrados.add(evento);
        }

        @Override
        public List<EventoIot> listarDoAluno(Long alunoId) {
            return registrados.stream().filter(e -> e.alunoId().equals(alunoId)).toList();
        }
    }

    private final EventosEmMemoria eventos = new EventosEmMemoria();
    private final List<Long> cursosConcluidos = new ArrayList<>();

    private final ConclusaoDeCurso conclusao = alunoId -> {
        if (alunoId != ALUNO) {
            return Optional.empty();
        }
        cursosConcluidos.add(alunoId);
        return Optional.of(new ProgressoAluno(alunoId, "BASICO", cursosConcluidos.size()));
    };

    private final ProgressoIndividualConsulta progresso =
            alunoId -> alunoId == ALUNO ? Optional.of(new ProgressoAluno(ALUNO, "BASICO", 0)) : Optional.empty();

    private final RegistroEventoIotService servico = new RegistroEventoIotService(eventos, conclusao, progresso);

    private static EventoIot evento(String id, long alunoId, TipoEventoIot tipo) {
        return new EventoIot(id, "lab-01", "leitor-07", alunoId, tipo, AGORA);
    }

    @Test
    void conclusaoDeAtividadeConcluiOCursoERegistraOEvento() {
        assertEquals(Resultado.REGISTRADO, servico.registrar(evento("e1", ALUNO, TipoEventoIot.CONCLUSAO_ATIVIDADE)));

        assertEquals(List.of(ALUNO), cursosConcluidos);
        assertEquals(1, servico.eventosDoAluno(ALUNO).size());
    }

    @Test
    void mesmoEventoRetransmitidoContaUmaVezSo() {
        EventoIot conclusaoRepetida = evento("e1", ALUNO, TipoEventoIot.CONCLUSAO_ATIVIDADE);

        servico.registrar(conclusaoRepetida);
        assertEquals(Resultado.DUPLICADO, servico.registrar(conclusaoRepetida));
        assertEquals(Resultado.DUPLICADO, servico.registrar(conclusaoRepetida));

        assertEquals(1, cursosConcluidos.size());
        assertEquals(1, eventos.registrados.size());
    }

    @Test
    void presencaEInicioSaoRegistradosSemConcluirCurso() {
        servico.registrar(evento("p1", ALUNO, TipoEventoIot.PRESENCA));
        servico.registrar(evento("i1", ALUNO, TipoEventoIot.INICIO_ATIVIDADE));

        assertTrue(cursosConcluidos.isEmpty());
        assertEquals(2, servico.eventosDoAluno(ALUNO).size());
    }

    @Test
    void eventoDeAlunoInexistenteNaoEhRegistrado() {
        assertEquals(Resultado.ALUNO_INEXISTENTE, servico.registrar(evento("e9", 99L, TipoEventoIot.CONCLUSAO_ATIVIDADE)));
        assertEquals(Resultado.ALUNO_INEXISTENTE, servico.registrar(evento("p9", 99L, TipoEventoIot.PRESENCA)));

        assertTrue(eventos.registrados.isEmpty());
    }
}
