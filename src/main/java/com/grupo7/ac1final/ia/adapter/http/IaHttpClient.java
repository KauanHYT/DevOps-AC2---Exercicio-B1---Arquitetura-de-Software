package com.grupo7.ac1final.ia.adapter.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.ia.application.port.AssistenteIaPort;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.application.port.RecomendacaoIaPort;
import com.grupo7.ac1final.ia.domain.ItemRecomendado;
import com.grupo7.ac1final.ia.domain.RecomendacaoDoModelo;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * Adapter de saída HTTP para o ia-service (Python/FastAPI), ADR-003.
 * - timeouts curtos e separados por capacidade: recomendação é rápida, o assistente (LLM) é lento;
 * - qualquer falha técnica vira IaIndisponivelException, e a aplicação decide o fallback;
 * - leitor tolerante: campos novos na resposta do Python não quebram o Core.
 */
@Component
public class IaHttpClient implements RecomendacaoIaPort, AssistenteIaPort {

    private final RestClient clienteRecomendacao;
    private final RestClient clienteAssistente;

    public IaHttpClient(@Value("${ia.url}") String urlBase,
                        @Value("${ia.timeout.conexao-ms:300}") long conexaoMs,
                        @Value("${ia.timeout.recomendacao-ms:800}") long recomendacaoMs,
                        @Value("${ia.timeout.assistente-ms:5000}") long assistenteMs) {
        this.clienteRecomendacao = criarCliente(urlBase, conexaoMs, recomendacaoMs);
        this.clienteAssistente = criarCliente(urlBase, conexaoMs, assistenteMs);
    }

    @Override
    public RecomendacaoDoModelo recomendar(ProgressoAluno progresso, int limite) {
        try {
            RespostaRecomendacaoIa resposta = clienteRecomendacao.post()
                    .uri("/recomendacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PedidoRecomendacaoIa(progresso.alunoId(), progresso.plano(),
                            progresso.cursosConcluidos(), limite))
                    .retrieve()
                    .body(RespostaRecomendacaoIa.class);

            if (resposta == null || resposta.itens() == null) {
                throw new IaIndisponivelException("ia-service devolveu resposta vazia");
            }
            List<ItemRecomendado> itens = resposta.itens().stream()
                    .map(i -> new ItemRecomendado(i.cursoId(), i.titulo(), i.motivo()))
                    .toList();
            return new RecomendacaoDoModelo(resposta.modeloVersao(), itens);
        } catch (RestClientException e) {
            throw new IaIndisponivelException("falha ao chamar /recomendacoes: " + e.getMessage(), e);
        }
    }

    @Override
    public RespostaAssistente perguntar(ProgressoAluno contexto, String pergunta) {
        try {
            RespostaAssistenteIa resposta = clienteAssistente.post()
                    .uri("/assistente/perguntas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PerguntaIa(contexto.alunoId(), contexto.plano(), pergunta))
                    .retrieve()
                    .body(RespostaAssistenteIa.class);

            if (resposta == null || resposta.resposta() == null) {
                throw new IaIndisponivelException("ia-service devolveu resposta vazia");
            }
            List<String> fontes = resposta.fontes() == null ? List.of() : resposta.fontes();
            return new RespostaAssistente(resposta.resposta(), fontes, resposta.modeloVersao());
        } catch (RestClientException e) {
            throw new IaIndisponivelException("falha ao chamar /assistente/perguntas: " + e.getMessage(), e);
        }
    }

    private static RestClient criarCliente(String urlBase, long conexaoMs, long leituraMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(conexaoMs));
        fabrica.setReadTimeout(Duration.ofMillis(leituraMs));
        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .build();
    }

    // ---------- contrato HTTP com o ia-service (espelha ia-service/app/main.py) ----------

    public record PedidoRecomendacaoIa(Long alunoId, String plano, int cursosConcluidos, int limite) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemIa(String cursoId, String titulo, String motivo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaRecomendacaoIa(Long alunoId, String modeloVersao, List<ItemIa> itens) {
    }

    public record PerguntaIa(Long alunoId, String plano, String pergunta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaAssistenteIa(String resposta, List<String> fontes, String modeloVersao) {
    }
}