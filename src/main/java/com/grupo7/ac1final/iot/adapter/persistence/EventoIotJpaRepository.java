package com.grupo7.ac1final.iot.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoIotJpaRepository extends JpaRepository<EventoIotEntity, Long> {

    boolean existsByEventoId(String eventoId);

    List<EventoIotEntity> findByAlunoIdOrderByOcorridoEmAsc(Long alunoId);
}
