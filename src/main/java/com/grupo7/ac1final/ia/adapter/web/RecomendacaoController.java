package com.grupo7.ac1final.ia.adapter.web;

import com.grupo7.ac1final.ia.application.RecomendacaoService;
import com.grupo7.ac1final.ia.domain.Recomendacoes;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF03. Sempre 200 para aluno existente: a origem (IA ou PADRAO) vai no corpo. */
@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RecomendacaoController {

    /** Desafio 6: quantas recomendações saíram da IA e quantas da trilha padrão (ASR-07). */
    public static final String RECOMENDACOES = "gamificacao.recomendacoes";

    private final RecomendacaoService recomendacaoService;
    private final MeterRegistry metricas;

    public RecomendacaoController(RecomendacaoService recomendacaoService, MeterRegistry metricas) {
        this.recomendacaoService = recomendacaoService;
        this.metricas = metricas;
    }

    @GetMapping("/alunos/{id}/recomendacoes")
    public ResponseEntity<Recomendacoes> recomendar(@PathVariable("id") Long id,
                                                    @RequestParam(name = "limite", defaultValue = "3") int limite) {
        return recomendacaoService.recomendar(id, limite)
                .map(recomendacoes -> {
                    contar(recomendacoes);
                    return ResponseEntity.ok(recomendacoes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void contar(Recomendacoes recomendacoes) {
        Counter.builder(RECOMENDACOES)
                .description("Recomendações entregues, por origem: IA ou trilha PADRAO (ADR-003)")
                .tag("origem", recomendacoes.origem().name())
                .register(metricas)
                .increment();
    }
}