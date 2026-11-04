package com.grupo7.ac1final.observabilidade;

import com.grupo7.ac1final.aluno.adapter.persistence.outbox.OutboxMetricas;
import com.grupo7.ac1final.ia.adapter.http.ServidorIaFalso;
import com.grupo7.ac1final.ia.adapter.web.RecomendacaoController;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Desafio 6: as métricas que o Prometheus lê comprovam decisões anteriores.
 * Cada teste provoca a condição e confere a métrica que deve denunciá-la.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MetricasArquiteturaIntegrationTest {

    private static final ServidorIaFalso IA = new ServidorIaFalso();

    @DynamicPropertySource
    static void apontarParaIaFalsa(DynamicPropertyRegistry registro) {
        registro.add("ia.url", IA::url);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry metricas;

    @Test
    void prometheusExpoeAsMetricasQueComprovamOsAdrs() throws Exception {
        mockMvc.perform(put("/alunos/{id}/concluir-curso", criarAluno())).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gamificacao_outbox_pendentes")))
                .andExpect(content().string(containsString("http_server_requests_seconds")))
                .andExpect(content().string(containsString("uri=\"/alunos/{id}/concluir-curso\"")))
                .andExpect(content().string(containsString("application=\"ac1final\"")));
    }

    @Test
    void promocaoAindaNaoPublicadaApareceNoGaugeDoOutbox() throws Exception {
        // nos testes o relay fica desligado: é o mesmo que o RabbitMQ estar fora do ar
        long id = criarAluno();
        double antes = pendentesNoOutbox();

        for (int i = 0; i < 12; i++) {
            mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isOk());
        }

        assertEquals(antes + 1, pendentesNoOutbox(), "a promoção do 12º curso fica esperando no outbox");
    }

    @Test
    void quedaDaIaApareceComoRecomendacaoPadrao() throws Exception {
        long id = criarAluno();
        double iaAntes = recomendacoes("IA");
        double padraoAntes = recomendacoes("PADRAO");

        mockMvc.perform(get("/alunos/{id}/recomendacoes", id)).andExpect(jsonPath("$.origem").value("IA"));
        IA.desligar();
        mockMvc.perform(get("/alunos/{id}/recomendacoes", id)).andExpect(jsonPath("$.origem").value("PADRAO"));

        assertEquals(iaAntes + 1, recomendacoes("IA"));
        assertEquals(padraoAntes + 1, recomendacoes("PADRAO"));
    }

    private double pendentesNoOutbox() {
        return metricas.get(OutboxMetricas.PENDENTES).gauge().value();
    }

    private double recomendacoes(String origem) {
        Counter contador = metricas.find(RecomendacaoController.RECOMENDACOES).tag("origem", origem).counter();
        return contador == null ? 0 : contador.count();
    }

    private long criarAluno() throws Exception {
        String corpo = mockMvc.perform(post("/alunos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"BASICO\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }
}