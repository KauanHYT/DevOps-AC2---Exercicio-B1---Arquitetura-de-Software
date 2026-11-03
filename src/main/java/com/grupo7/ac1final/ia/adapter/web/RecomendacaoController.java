package com.grupo7.ac1final.ia.adapter.web;

import com.grupo7.ac1final.ia.application.RecomendacaoService;
import com.grupo7.ac1final.ia.domain.Recomendacoes;
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

    private final RecomendacaoService recomendacaoService;

    public RecomendacaoController(RecomendacaoService recomendacaoService) {
        this.recomendacaoService = recomendacaoService;
    }

    @GetMapping("/alunos/{id}/recomendacoes")
    public ResponseEntity<Recomendacoes> recomendar(@PathVariable("id") Long id,
                                                    @RequestParam(name = "limite", defaultValue = "3") int limite) {
        return recomendacaoService.recomendar(id, limite)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}