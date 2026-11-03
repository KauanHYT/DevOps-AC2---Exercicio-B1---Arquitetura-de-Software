package com.grupo7.ac1final.aluno.api;

import java.util.Optional;

/**
 * API pública do módulo aluno para consultar o progresso de UM aluno (ADR-001/ADR-003).
 * Usada pelo módulo ia para montar o contexto enviado ao serviço Python, sem que ele
 * acesse entidade, repositório ou banco do aluno.
 */
public interface ProgressoIndividualConsulta {

    Optional<ProgressoAluno> buscarProgresso(Long alunoId);
}