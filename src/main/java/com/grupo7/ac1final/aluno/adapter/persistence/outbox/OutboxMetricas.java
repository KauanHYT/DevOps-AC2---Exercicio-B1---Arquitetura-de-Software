package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetricas implements MeterBinder {

    public static final String PENDENTES = "gamificacao.outbox.pendentes";

    private final OutboxStore outbox;

    public OutboxMetricas(OutboxStore outbox) {
        this.outbox = outbox;
    }

    @Override
    public void bindTo(MeterRegistry registro) {
        Gauge.builder(PENDENTES, outbox, OutboxStore::contarPendentes)
                .description("Eventos gravados no outbox e ainda não publicados no RabbitMQ (ADR-004)")
                .register(registro);
    }
}