package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;
import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "notificacao.modo", havingValue = "assincrono", matchIfMissing = true)
public class OutboxNotificadorPromocao implements NotificadorPromocao {

    static final String TIPO = "AlunoPromovidoPremium";

    private final EventoOutboxJpaRepository outbox;

    public OutboxNotificadorPromocao(EventoOutboxJpaRepository outbox) {
        this.outbox = outbox;
    }

    @Override
    public Resultado notificar(AlunoPromovidoPremium evento) {
        outbox.save(new EventoOutboxEntity(evento.eventoId(), TIPO, paraJson(evento), Instant.now()));
        return Resultado.AGENDADA;
    }

    static String paraJson(AlunoPromovidoPremium evento) {
        return "{\"eventoId\":\"%s\",\"tipo\":\"%s\",\"alunoId\":%d,\"cursosConcluidos\":%d,\"ocorridoEm\":\"%s\"}"
                .formatted(evento.eventoId(), TIPO, evento.alunoId(), evento.cursosConcluidos(), evento.ocorridoEm());
    }
}