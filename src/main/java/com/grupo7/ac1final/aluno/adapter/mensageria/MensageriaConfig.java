package com.grupo7.ac1final.aluno.adapter.mensageria;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "outbox.relay.habilitado", havingValue = "true")
public class MensageriaConfig {

    @Bean
    public Declarables topologiaNotificacao() {
        TopicExchange exchange = new TopicExchange(TopologiaRabbit.EXCHANGE, true, false);
        Queue fila = new Queue(TopologiaRabbit.FILA_NOTIFICACAO, true);
        return new Declarables(exchange, fila,
                BindingBuilder.bind(fila).to(exchange).with(TopologiaRabbit.ROTA_ALUNO_PROMOVIDO));
    }
}