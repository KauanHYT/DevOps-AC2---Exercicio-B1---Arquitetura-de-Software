package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxStore {

    private final EventoOutboxJpaRepository repositorio;

    public OutboxStore(EventoOutboxJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<EventoPendente> pendentes(int limite) {
        return repositorio.findByPublicadoEmIsNullOrderByCriadoEmAsc(PageRequest.of(0, limite))
                .stream()
                .map(e -> new EventoPendente(e.getId(), e.getTipo(), e.getPayload(), e.getTentativas()))
                .toList();
    }

    @Transactional
    public void marcarPublicado(String id) {
        repositorio.findById(id).ifPresent(e -> e.marcarPublicado(Instant.now()));
    }

    @Transactional
    public void registrarFalha(String id) {
        repositorio.findById(id).ifPresent(EventoOutboxEntity::registrarFalha);
    }

    @Transactional(readOnly = true)
    public long contarPendentes() {
        return repositorio.countByPublicadoEmIsNull();
    }
}