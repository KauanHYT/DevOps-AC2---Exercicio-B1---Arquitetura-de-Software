package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoOutboxJpaRepository extends JpaRepository<EventoOutboxEntity, String> {

    List<EventoOutboxEntity> findByPublicadoEmIsNullOrderByCriadoEmAsc(Pageable pageable);

    long countByPublicadoEmIsNull();
}