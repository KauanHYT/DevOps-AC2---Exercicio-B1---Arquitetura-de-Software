package com.grupo7.ac1final.aluno.adapter.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Detalhe técnico do adapter JPA: só AlunoPersistenceAdapter usa esta interface. */
public interface AlunoJpaRepository extends JpaRepository<AlunoEntity, Long> {

    /** Consulta paginada no banco: o ranking não carrega todos os alunos em memória. */
    List<AlunoEntity> findAllByOrderByCursosConcluidosDescIdAsc(Pageable pageable);
}
