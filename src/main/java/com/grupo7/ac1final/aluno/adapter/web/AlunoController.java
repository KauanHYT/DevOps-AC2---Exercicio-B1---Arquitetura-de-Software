package com.grupo7.ac1final.aluno.adapter.web;

import com.grupo7.ac1final.aluno.application.AlunoService;
import com.grupo7.ac1final.aluno.application.DadosCadastroAluno;
import com.grupo7.ac1final.aluno.application.port.NotificacaoIndisponivelException;
import com.grupo7.ac1final.aluno.domain.Aluno;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter de entrada HTTP (ADR-002): traduz requisição -&gt; caso de uso -&gt; DTO.
 * Não contém regra de negócio nem conhece JPA.
 */
@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping
    public ResponseEntity<AlunoDTO> criar(@RequestBody AlunoDTO dto) {
        Aluno salvo = alunoService.cadastrar(new DadosCadastroAluno(
                dto.getPlano(),
                dto.getCursosConcluidos(),
                dto.getCursosAdicionaisPermitidos(),
                dto.isPossuiVoucher(),
                dto.isNotificadoMelhoriaPlano()));

        return ResponseEntity.ok(AlunoDTO.de(salvo));
    }

    @GetMapping
    public ResponseEntity<List<AlunoDTO>> listarTodos() {
        List<AlunoDTO> alunos = alunoService.listarTodos()
                .stream()
                .map(AlunoDTO::de)
                .toList();

        return ResponseEntity.ok(alunos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoDTO> buscarPorId(@PathVariable Long id) {
        return resposta(alunoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        return alunoService.remover(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/concluir-curso")
    public ResponseEntity<AlunoDTO> concluirCurso(@PathVariable Long id) {
        return resposta(alunoService.concluirCurso(id));
    }

    /** Agora recebe a média: a regra "média >= 7,0" (US 1/US 4) é aplicada pelo domínio. */
    @PutMapping("/{id}/liberar-cursos")
    public ResponseEntity<AlunoDTO> liberarCursos(@PathVariable Long id,
                                                  @RequestParam("media") double media) {
        return resposta(alunoService.liberarCursosExtras(id, media));
    }

    @PutMapping("/{id}/notificar")
    public ResponseEntity<AlunoDTO> notificarMudancaPlano(@PathVariable Long id) {
        return resposta(alunoService.registrarNotificacaoMudancaPlano(id));
    }

    /**
     * Só acontece no modo síncrono (alternativa A do ADR-004): a notificação falhou e,
     * junto com ela, a conclusão do curso foi desfeita.
     */
    @ExceptionHandler(NotificacaoIndisponivelException.class)
    public ResponseEntity<Map<String, String>> notificacaoIndisponivel() {
        return ResponseEntity.status(503).body(Map.of(
                "mensagem", "Serviço de notificação indisponível: a conclusão do curso não foi registrada."));
    }

    private ResponseEntity<AlunoDTO> resposta(Optional<Aluno> aluno) {
        return aluno
                .map(a -> ResponseEntity.ok(AlunoDTO.de(a)))
                .orElse(ResponseEntity.notFound().build());
    }
}