package com.grupo7.ac1final.aluno.adapter.mensageria;

import com.grupo7.ac1final.aluno.adapter.persistence.outbox.EventoPendente;
import com.grupo7.ac1final.aluno.adapter.persistence.outbox.OutboxStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "outbox.relay.habilitado", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int LOTE = 50;

    private final OutboxStore outbox;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxStore outbox, RabbitTemplate rabbit) {
        this.outbox = outbox;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.intervalo-ms:2000}")
    public void publicarPendentes() {
        for (EventoPendente evento : outbox.pendentes(LOTE)) {
            try {
                Message mensagem = MessageBuilder.withBody(evento.payload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(evento.id())
                        .build();
                rabbit.send(TopologiaRabbit.EXCHANGE, TopologiaRabbit.ROTA_ALUNO_PROMOVIDO, mensagem);
                outbox.marcarPublicado(evento.id());
                log.info("evento {} ({}) publicado no RabbitMQ", evento.id(), evento.tipo());
            } catch (AmqpException e) {
                outbox.registrarFalha(evento.id());
                log.warn("RabbitMQ indisponível: evento {} continua pendente no outbox (tentativa {}): {}",
                        evento.id(), evento.tentativas() + 1, e.getMessage());
                return;
            }
        }
    }
}