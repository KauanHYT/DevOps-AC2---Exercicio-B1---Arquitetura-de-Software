package com.grupo7.ac1final.iot.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventoIotTest {

    private static final Instant AGORA = Instant.parse("2026-11-04T12:00:00Z");

    @Test
    void soAConclusaoDeAtividadeConcluiCurso() {
        assertTrue(new EventoIot("e1", "lab-01", "leitor-07", 3L, TipoEventoIot.CONCLUSAO_ATIVIDADE, AGORA).concluiCurso());
        assertFalse(new EventoIot("e2", "lab-01", "leitor-07", 3L, TipoEventoIot.PRESENCA, AGORA).concluiCurso());
        assertFalse(new EventoIot("e3", "lab-01", "leitor-07", 3L, TipoEventoIot.INICIO_ATIVIDADE, AGORA).concluiCurso());
    }

    @Test
    void eventoIncompletoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventoIot(" ", "lab-01", "leitor-07", 3L, TipoEventoIot.PRESENCA, AGORA));
        assertThrows(IllegalArgumentException.class,
                () -> new EventoIot("e1", "lab-01", "leitor-07", 0L, TipoEventoIot.PRESENCA, AGORA));
        assertThrows(IllegalArgumentException.class,
                () -> new EventoIot("e1", "lab-01", "leitor-07", 3L, null, AGORA));
        assertThrows(IllegalArgumentException.class,
                () -> new EventoIot("x".repeat(65), "lab-01", "leitor-07", 3L, TipoEventoIot.PRESENCA, AGORA));
    }
}
