package com.grupo7.ac1final.aluno.adapter.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo HTTP -&gt; caso de uso -&gt; domínio -&gt; adapter JPA (H2), com o contexto
 * Spring real. Prova que a API agora executa as regras do domínio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlunoApiIntegrationTest {

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
    void liberarCursosRespeitaARegraDaMedia() throws Exception {
        long id = criarAluno();

        mockMvc.perform(put("/alunos/{id}/liberar-cursos", id).param("media", "6.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursosAdicionaisPermitidos").value(0));

        mockMvc.perform(put("/alunos/{id}/liberar-cursos", id).param("media", "7.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursosAdicionaisPermitidos").value(3));
    }

    @Test
    void dozeConclusoesTornamAlunoPremiumEOColocamNoTopoDoRanking() throws Exception {
        long id = criarAluno();
        for (int i = 0; i < 12; i++) {
            mockMvc.perform(put("/alunos/{id}/concluir-curso", id)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/alunos/{id}", id))
                .andExpect(jsonPath("$.plano").value("PREMIUM"))
                .andExpect(jsonPath("$.possuiVoucher").value(true));

        mockMvc.perform(get("/ranking").param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].posicao").value(1))
                .andExpect(jsonPath("$[0].cursosConcluidos").value(12));
    }

    @Test
    void alunoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/alunos/{id}/concluir-curso", 999_999)).andExpect(status().isNotFound());
        mockMvc.perform(get("/alunos/{id}", 999_999)).andExpect(status().isNotFound());
    }
}
