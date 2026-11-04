package com.grupo7.ac1final.aluno.adapter.http;

import com.grupo7.ac1final.aluno.application.port.NotificacaoIndisponivelException;
import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;
import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "notificacao.modo", havingValue = "sincrono")
public class NotificacaoHttpNotificador implements NotificadorPromocao {

    private final RestClient cliente;

    public NotificacaoHttpNotificador(@Value("${notificacao.url}") String urlBase,
                                      @Value("${notificacao.timeout-ms:2000}") long timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.cliente = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public Resultado notificar(AlunoPromovidoPremium evento) {
        try {
            cliente.post()
                    .uri("/notificacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificacaoHttp(evento.eventoId(), "AlunoPromovidoPremium", evento.alunoId(),
                            evento.cursosConcluidos(), evento.ocorridoEm().toString()))
                    .retrieve()
                    .toBodilessEntity();
            return Resultado.ENTREGUE;
        } catch (RestClientException e) {
            throw new NotificacaoIndisponivelException("serviço de notificação indisponível: " + e.getMessage(), e);
        }
    }

    public record NotificacaoHttp(String eventoId, String tipo, Long alunoId, int cursosConcluidos, String ocorridoEm) {
    }
}