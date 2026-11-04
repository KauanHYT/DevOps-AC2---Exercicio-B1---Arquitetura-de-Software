package com.grupo7.ac1final.aluno.adapter.persistence.outbox;

public record EventoPendente(String id, String tipo, String payload, int tentativas) {
}