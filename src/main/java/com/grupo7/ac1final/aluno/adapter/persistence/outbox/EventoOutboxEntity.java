package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "outbox_eventos")
public class EventoOutboxEntity {

    @Id
    private String id;

    private String tipo;

    @Column(length = 2000)
    private String payload;

    private Instant criadoEm;

    private Instant publicadoEm;

    private int tentativas;

    protected EventoOutboxEntity() {
    }

    public EventoOutboxEntity(String id, String tipo, String payload, Instant criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    public void marcarPublicado(Instant quando) {
        this.publicadoEm = quando;
    }

    public void registrarFalha() {
        this.tentativas++;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public int getTentativas() {
        return tentativas;
    }
}