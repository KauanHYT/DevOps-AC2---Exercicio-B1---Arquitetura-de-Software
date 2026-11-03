package com.grupo7.ac1final.ia.adapter.http;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.domain.RecomendacaoDoModelo;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adapter HTTP real (RestClient) contra um ia-service falso.
 * Mede o que o ADR-003 promete: falha rápida e limitada pelo timeout, nunca travar o Core.
 */
class IaHttpClientTest {

    private static final ProgressoAluno ALUNO = new ProgressoAluno(1L, "BASICO", 5);
    private static final long TIMEOUT_LEITURA_MS = 300;

    private IaHttpClient clientePara(String url) {
        return new IaHttpClient(url, 200, TIMEOUT_LEITURA_MS, TIMEOUT_LEITURA_MS);
    }

    @Test
    void traduzOContratoDoPythonIgnorandoCamposNovos() {
        try (ServidorIaFalso ia = new ServidorIaFalso()) {
            RecomendacaoDoModelo r = clientePara(ia.url()).recomendar(ALUNO, 3);

            assertEquals("regras-v1", r.modeloVersao());
            assertEquals("WEB-210", r.itens().get(0).cursoId());
        }
    }

    @Test
    void traduzARespostaDoAssistente() {
        try (ServidorIaFalso ia = new ServidorIaFalso()) {
            RespostaAssistente r = clientePara(ia.url()).perguntar(ALUNO, "como viro premium?");

            assertEquals(List.of("regra-premium"), r.fontes());
        }
    }

    @Test
    void modeloLentoEstouraOTimeoutEmVezDeSegurarOCore() {
        try (ServidorIaFalso ia = new ServidorIaFalso()) {
            ia.ficarLento(2_000);

            long inicio = System.nanoTime();
            boolean indisponivel = falhaComIndisponivel(() -> clientePara(ia.url()).recomendar(ALUNO, 3));
            long decorridoMs = (System.nanoTime() - inicio) / 1_000_000;

            assertTrue(indisponivel);
            assertTrue(decorridoMs < 1_500, "deveria falhar perto do timeout de 300 ms, levou " + decorridoMs + " ms");
        }
    }

    @Test
    void servicoDesligadoFalhaRapido() {
        ServidorIaFalso ia = new ServidorIaFalso();
        String url = ia.url();
        ia.desligar();

        long inicio = System.nanoTime();
        boolean indisponivel = falhaComIndisponivel(() -> clientePara(url).recomendar(ALUNO, 3));
        long decorridoMs = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(indisponivel);
        assertTrue(decorridoMs < 1_500, "conexão recusada deveria falhar rápido, levou " + decorridoMs + " ms");
    }

    @Test
    void erro500DoPythonViraIndisponivel() {
        try (ServidorIaFalso ia = new ServidorIaFalso()) {
            ia.responderComErro(500);

            assertTrue(falhaComIndisponivel(() -> clientePara(ia.url()).perguntar(ALUNO, "oi")));
        }
    }

    private static boolean falhaComIndisponivel(Runnable chamada) {
        try {
            chamada.run();
            return false;
        } catch (IaIndisponivelException e) {
            return true;
        }
    }
}