package com.grupo7.ac1final.ia.application;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.ia.application.port.AssistenteIaPort;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistenteServiceTest {

    private final ProgressoIndividualConsulta consulta =
            id -> id == 1L ? Optional.of(new ProgressoAluno(1L, "BASICO", 2)) : Optional.empty();

    @Test
    void enviaOContextoDoAlunoEDevolveAResposta() {
        AssistenteIaPort assistente = (contexto, pergunta) ->
                new RespostaAssistente("plano " + contexto.plano() + ": " + pergunta, List.of("regra-premium"), "rag-simulado-v1");

        RespostaAssistente r = new AssistenteService(consulta, assistente).perguntar(1L, "como viro premium?").orElseThrow();

        assertEquals("plano BASICO: como viro premium?", r.resposta());
        assertEquals(List.of("regra-premium"), r.fontes());
    }

    @Test
    void assistenteForaDoArPropagaIndisponibilidadeSemInventarResposta() {
        AssistenteIaPort fora = (contexto, pergunta) -> {
            throw new IaIndisponivelException("timeout");
        };
        AssistenteService service = new AssistenteService(consulta, fora);

        boolean indisponivel = false;
        try {
            service.perguntar(1L, "oi");
        } catch (IaIndisponivelException e) {
            indisponivel = true;
        }
        assertTrue(indisponivel);
    }

    @Test
    void alunoInexistenteRetornaVazio() {
        AssistenteIaPort qualquer = (contexto, pergunta) -> new RespostaAssistente("x", List.of(), "v");

        assertTrue(new AssistenteService(consulta, qualquer).perguntar(42L, "oi").isEmpty());
    }
}