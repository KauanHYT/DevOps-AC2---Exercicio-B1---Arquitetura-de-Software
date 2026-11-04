package com.grupo7.ac1final.aluno.adapter.http;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Desafio 4, alternativa (A) REST síncrono, que o ADR-004 rejeita: com o serviço de
 * notificação fora do ar, o 12º curso NÃO é registrado. O teste prova o acoplamento
 * que motivou a decisão.
 */
@SpringBootTest(properties = "notificacao.modo=sincrono")
@AutoConfigureMockMvc
class NotificacaoSincronaIntegrationTest {

    private static final ServidorNotificacaoFalso NOTIFICACAO = new ServidorNotificacaoFalso();

    @DynamicPropertySource
    static void apontarParaNotificacaoFalsa(DynamicPropertyRegistry registro) {
        registro.add("notificacao.url", NOTIFICACAO::url);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void comConsumidorForaDoArAConclusaoEDesfeitaEOAlunoRecebe503() throws Exception {
        String corpo = mockMvc.perform(post("/alunos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"BASICO\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
        for (int i = 0; i < 11; i++) {
            mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isOk());
        }

        NOTIFICACAO.ficarForaDoAr();
        mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/alunos/{id}", id))
                .andExpect(jsonPath("$.cursosConcluidos").value(11))
                .andExpect(jsonPath("$.plano").value("BASICO"));

        NOTIFICACAO.voltar();
        mockMvc.perform(put("/alunos/{id}/concluir-curso", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("PREMIUM"))
                .andExpect(jsonPath("$.notificadoMelhoriaPlano").value(true));
        assertEquals(1, NOTIFICACAO.recebidas());
    }
}