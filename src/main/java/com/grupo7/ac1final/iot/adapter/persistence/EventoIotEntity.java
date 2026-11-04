package com.grupo7.ac1final.iot.adapter.persistence;

import com.grupo7.ac1final.iot.domain.EventoIot;
import com.grupo7.ac1final.iot.domain.TipoEventoIot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Evento IoT já processado (ADR-005). A coluna evento_id é UNIQUE: mesmo que dois envios
 * do mesmo evento cheguem ao mesmo tempo, o banco aceita só um.
 */
@Entity
@Table(name = "eventos_iot")
public class EventoIotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = EventoIot.TAMANHO_MAXIMO_ID)
    private String eventoId;

    private String laboratorio;

    private String dispositivoId;

    private Long alunoId;

    @Enumerated(EnumType.STRING)
    private TipoEventoIot tipo;

    private Instant ocorridoEm;

    private Instant recebidoEm;

    protected EventoIotEntity() {
    }

    public static EventoIotEntity de(EventoIot evento, Instant recebidoEm) {
        EventoIotEntity entidade = new EventoIotEntity();
        entidade.eventoId = evento.eventoId();
        entidade.laboratorio = evento.laboratorio();
        entidade.dispositivoId = evento.dispositivoId();
        entidade.alunoId = evento.alunoId();
        entidade.tipo = evento.tipo();
        entidade.ocorridoEm = evento.ocorridoEm();
        entidade.recebidoEm = recebidoEm;
        return entidade;
    }

    public EventoIot paraDominio() {
        return new EventoIot(eventoId, laboratorio, dispositivoId, alunoId, tipo, ocorridoEm);
    }
}
