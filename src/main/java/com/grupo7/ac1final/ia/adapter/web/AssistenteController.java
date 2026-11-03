package com.grupo7.ac1final.ia.adapter.web;

import com.grupo7.ac1final.ia.application.AssistenteService;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** RF04. 503 quando o assistente está fora: indisponibilidade explícita, sem resposta inventada. */
@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class AssistenteController {

    static final int TAMANHO_MAXIMO_PERGUNTA = 500;

    private final AssistenteService assistenteService;

    public AssistenteController(AssistenteService assistenteService) {
        this.assistenteService = assistenteService;
    }

    public record PerguntaDTO(String pergunta) {
    }

    @PostMapping("/alunos/{id}/assistente")
    public ResponseEntity<?> perguntar(@PathVariable("id") Long id, @RequestBody PerguntaDTO corpo) {
        if (corpo == null || corpo.pergunta() == null || corpo.pergunta().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Informe a pergunta."));
        }
        if (corpo.pergunta().length() > TAMANHO_MAXIMO_PERGUNTA) {
            // mesmo limite do contrato do ia-service; sem isso o 422 do Python viraria um falso "indisponível"
            return ResponseEntity.badRequest().body(Map.of("mensagem", "A pergunta pode ter até 500 caracteres."));
        }
        return assistenteService.perguntar(id, corpo.pergunta().trim())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<Map<String, String>> assistenteIndisponivel() {
        return ResponseEntity.status(503).body(Map.of(
                "mensagem", "Assistente indisponível no momento. As demais funções da plataforma continuam normais."));
    }
}