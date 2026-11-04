package com.grupo7.ac1final.iot.adapter.persistence;

import com.grupo7.ac1final.iot.application.port.EventoIotRepositoryPort;
import com.grupo7.ac1final.iot.domain.EventoIot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Adapter de saída: implementa a porta de eventos IoT com JPA (ADR-002 aplicado ao módulo iot). */
@Component
public class EventoIotPersistenceAdapter implements EventoIotRepositoryPort {

    private final EventoIotJpaRepository repositorio;

    public EventoIotPersistenceAdapter(EventoIotJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean jaRegistrado(String eventoId) {
        return repositorio.existsByEventoId(eventoId);
    }

    @Override
    public void registrar(EventoIot evento) {
        repositorio.save(EventoIotEntity.de(evento, Instant.now()));
    }

    @Override
    public List<EventoIot> listarDoAluno(Long alunoId) {
        return repositorio.findByAlunoIdOrderByOcorridoEmAsc(alunoId).stream()
                .map(EventoIotEntity::paraDominio)
                .toList();
    }
}
