package com.grupo7.ac1final.aluno.adapter.web;

import com.grupo7.ac1final.aluno.adapter.persistence.outbox.OutboxStore;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Desafio 4, modo assíncrono (decisão do ADR-004), sem broker e sem consumidor rodando:
 * a conclusão responde 200, a recompensa é imediata e o evento fica guardado no outbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificacaoAssincronaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxStore outbox;

    @Test
    void conclusaoNaoDependeDoConsumidorENotificacaoChegaDepois() throws Exception {
        String corpo = mockMvc.perform(post("/alunos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"BASICO\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        long pendentesAntes = outbox.contarPendentes();

        for (int i = 0; i < 12; i++) {
            mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isOk());
        }
        mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isOk());

        // recompensa imediata e consistente; notificação ainda não entregue
        mockMvc.perform(get("/alunos/{id}", id))
                .andExpect(jsonPath("$.plano").value("PREMIUM"))
                .andExpect(jsonPath("$.possuiVoucher").value(true))
                .andExpect(jsonPath("$.notificadoMelhoriaPlano").value(false));
        assertEquals(pendentesAntes + 1, outbox.contarPendentes(), "um único evento, guardado no outbox");

        // o que o notificacao-service faz depois de entregar a notificação
        mockMvc.perform(put("/alunos/{id}/notificar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificadoMelhoriaPlano").value(true));
    }
}