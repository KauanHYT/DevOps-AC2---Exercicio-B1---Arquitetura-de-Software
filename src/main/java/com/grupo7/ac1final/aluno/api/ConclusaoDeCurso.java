package com.grupo7.ac1final.aluno.api;

import java.util.Optional;

/**
 * API pública do módulo aluno para registrar a conclusão de um curso vinda de outro
 * módulo (Desafio 5: eventos IoT dos laboratórios). Quem chama não conhece a entidade,
 * o serviço nem o banco do aluno; as regras (12 cursos -> PREMIUM, evento do ADR-004)
 * continuam todas dentro do módulo aluno.
 */
public interface ConclusaoDeCurso {

    /** Conclui um curso do aluno e devolve o progresso atualizado, ou vazio se o aluno não existir. */
    Optional<ProgressoAluno> concluirCursoDoAluno(Long alunoId);
}
