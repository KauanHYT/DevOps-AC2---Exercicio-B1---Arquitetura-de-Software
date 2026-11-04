package com.grupo7.ac1final.iot.application.port;

import com.grupo7.ac1final.iot.domain.EventoIot;

import java.util.List;

/**
 * Porta de saída: onde ficam os eventos IoT já processados (ADR-005).
 * Também é a memória da deduplicação: um `eventoId` registrado não é processado de novo.
 */
public interface EventoIotRepositoryPort {

    boolean jaRegistrado(String eventoId);

    void registrar(EventoIot evento);

    List<EventoIot> listarDoAluno(Long alunoId);
}
