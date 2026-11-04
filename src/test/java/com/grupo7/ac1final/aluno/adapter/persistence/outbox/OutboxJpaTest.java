package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;
import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({OutboxNotificadorPromocao.class, OutboxStore.class})
class OutboxJpaTest {

    @Autowired
    private OutboxNotificadorPromocao notificador;

    @Autowired
    private OutboxStore outbox;

    private AlunoPromovidoPremium evento(String id) {
        return new AlunoPromovidoPremium(id, 1L, 12, Instant.now());
    }

    @Test
    void notificarAgendaOEventoNoOutbox() {
        assertEquals(NotificadorPromocao.Resultado.AGENDADA, notificador.notificar(evento("e-1")));

        List<EventoPendente> pendentes = outbox.pendentes(10);
        assertEquals(1, pendentes.size());
        assertEquals("e-1", pendentes.get(0).id());
        assertTrue(pendentes.get(0).payload().contains("\"alunoId\":1"));
    }

    @Test
    void eventoPublicadoSaiDaListaDePendentes() {
        notificador.notificar(evento("e-1"));
        notificador.notificar(evento("e-2"));

        outbox.marcarPublicado("e-1");

        assertEquals(1, outbox.contarPendentes());
        assertEquals("e-2", outbox.pendentes(10).get(0).id());
    }

    @Test
    void falhaDePublicacaoMantemOEventoPendenteEContaTentativa() {
        notificador.notificar(evento("e-1"));

        outbox.registrarFalha("e-1");

        assertEquals(1, outbox.contarPendentes());
        assertEquals(1, outbox.pendentes(10).get(0).tentativas());
    }
}