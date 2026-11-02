package com.grupo7.ac1final.aluno.api;

import java.util.List;

/**
 * API pública do módulo aluno para consulta de progresso (ADR-001).
 * Outros módulos dependem desta interface, nunca das classes internas
 * (controller, service, entidade JPA ou repositório).
 */
public interface ProgressoAlunoConsulta {

    /**
     * Alunos com mais cursos concluídos, em ordem decrescente.
     * Em caso de empate, o menor id vem primeiro.
     */
    List<ProgressoAluno> listarMaioresProgressos(int limite);
}
