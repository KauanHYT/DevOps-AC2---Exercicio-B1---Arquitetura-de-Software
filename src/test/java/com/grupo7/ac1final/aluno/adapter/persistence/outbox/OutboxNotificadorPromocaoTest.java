package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxNotificadorPromocaoTest {

    @Test
    void mensagemSegueOContratoDoConsumidor() {
        AlunoPromovidoPremium evento = new AlunoPromovidoPremium(
                "e-123", 7L, 12, Instant.parse("2026-11-03T12:00:00Z"));

        assertEquals("{\"eventoId\":\"e-123\",\"tipo\":\"AlunoPromovidoPremium\",\"alunoId\":7,"
                        + "\"cursosConcluidos\":12,\"ocorridoEm\":\"2026-11-03T12:00:00Z\"}",
                OutboxNotificadorPromocao.paraJson(evento));
    }
}