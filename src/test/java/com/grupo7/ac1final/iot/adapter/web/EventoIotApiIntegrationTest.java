package com.grupo7.ac1final.iot.adapter.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Desafio 5: o Core completo (Spring + H2) recebendo eventos já traduzidos pelo iot-gateway.
 * Prova a deduplicação por eventoId (QoS 1 = pelo menos uma vez) e a integração com as regras do aluno.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventoIotApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retransmissaoDoMesmoEventoNaoContaDuasVezes() throws Exception {
        long aluno = criarAluno();
        String conclusao = evento("e-" + UUID.randomUUID(), aluno, "CONCLUSAO_ATIVIDADE");

        enviar(conclusao).andExpect(status().isCreated()).andExpect(jsonPath("$.resultado").value("REGISTRADO"));
        enviar(conclusao).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("DUPLICADO"));
        enviar(conclusao).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("DUPLICADO"));

        mockMvc.perform(get("/alunos/{id}", aluno)).andExpect(jsonPath("$.cursosConcluidos").value(1));
    }

    @Test
    void dozeConclusoesVindasDoLaboratorioPromovemParaPremium() throws Exception {
        long aluno = criarAluno();
        enviar(evento("p-" + UUID.randomUUID(), aluno, "PRESENCA", "2026-11-04T11:00:00Z")).andExpect(status().isCreated());
        for (int i = 0; i < 12; i++) {
            enviar(evento("c-" + UUID.randomUUID(), aluno, "CONCLUSAO_ATIVIDADE")).andExpect(status().isCreated());
        }

        mockMvc.perform(get("/alunos/{id}", aluno))
                .andExpect(jsonPath("$.cursosConcluidos").value(12))
                .andExpect(jsonPath("$.plano").value("PREMIUM"))
                .andExpect(jsonPath("$.possuiVoucher").value(true));
        mockMvc.perform(get("/iot/eventos").param("alunoId", String.valueOf(aluno)))
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].tipo").value("PRESENCA"));
    }

    @Test
    void alunoInexistenteEEventoInvalidoSaoRecusados() throws Exception {
        enviar(evento("x-" + UUID.randomUUID(), 999_999, "CONCLUSAO_ATIVIDADE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultado").value("ALUNO_INEXISTENTE"));
        enviar(evento("y-" + UUID.randomUUID(), criarAluno(), "TIPO_QUE_NAO_EXISTE"))
                .andExpect(status().isBadRequest());
        enviar("{\"laboratorio\":\"lab-01\",\"dispositivoId\":\"leitor-07\",\"alunoId\":1,"
                + "\"tipo\":\"PRESENCA\",\"ocorridoEm\":\"2026-11-04T12:00:00Z\"}")
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/iot/eventos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String evento(String eventoId, long alunoId, String tipo) {
        return evento(eventoId, alunoId, tipo, "2026-11-04T12:00:00Z");
    }

    private static String evento(String eventoId, long alunoId, String tipo, String ocorridoEm) {
        return "{\"eventoId\":\"" + eventoId + "\",\"laboratorio\":\"lab-01\",\"dispositivoId\":\"leitor-07\","
                + "\"alunoId\":" + alunoId + ",\"tipo\":\"" + tipo + "\",\"ocorridoEm\":\"" + ocorridoEm + "\"}";
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
