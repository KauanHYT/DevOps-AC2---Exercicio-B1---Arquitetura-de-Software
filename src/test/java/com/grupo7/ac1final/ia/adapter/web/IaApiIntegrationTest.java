package com.grupo7.ac1final.ia.adapter.web;

import com.grupo7.ac1final.ia.adapter.http.ServidorIaFalso;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mini-lab do Desafio 3 automatizado: Core real (Spring + H2) consumindo um ia-service
 * que é desligado no meio do teste. Prova que a falha da IA não bloqueia o fluxo essencial.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IaApiIntegrationTest {

    private static final ServidorIaFalso IA = new ServidorIaFalso();

    @DynamicPropertySource
    static void apontarParaIaFalsa(DynamicPropertyRegistry registro) {
        registro.add("ia.url", IA::url);
    }

    @Autowired
    private MockMvc mockMvc;

    private long criarAluno() throws Exception {
        String corpo = mockMvc.perform(post("/alunos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"BASICO\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(corpo, "$.id");
        return id.longValue();
    }

    @Test
    void iaForaDoArDegradaRecomendacaoIsolaAssistenteENaoAfetaConclusaoDeCurso() throws Exception {
        long id = criarAluno();

        // 1) IA no ar: recomendação personalizada e assistente respondendo
        mockMvc.perform(get("/alunos/{id}/recomendacoes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origem").value("IA"))
                .andExpect(jsonPath("$.itens[0].cursoId").value("WEB-210"));
        mockMvc.perform(post("/alunos/{id}/assistente", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"como viro premium?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontes[0]").value("regra-premium"));

        // 2) IA cai
        IA.desligar();

        mockMvc.perform(get("/alunos/{id}/recomendacoes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origem").value("PADRAO"));
        mockMvc.perform(post("/alunos/{id}/assistente", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"como viro premium?\"}"))
                .andExpect(status().isServiceUnavailable());

        // 3) o fluxo essencial do Core continua funcionando
        mockMvc.perform(put("/alunos/{id}/concluir-curso", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursosConcluidos").value(1));
    }

    @Test
    void recomendacaoParaAlunoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/alunos/{id}/recomendacoes", 999_999)).andExpect(status().isNotFound());
    }
}