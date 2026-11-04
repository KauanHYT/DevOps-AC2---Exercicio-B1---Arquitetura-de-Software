package com.grupo7.ac1final.iot;

import java.time.Instant;

/**
 * Evento de um dispositivo, já no formato da plataforma (o gateway traduz o formato
 * compacto do dispositivo para este). O `eventoId` é gerado pelo dispositivo e é a
 * chave da deduplicação: retransmissões (QoS 1, reconexões) chegam com o mesmo id.
 */
public record EventoIot(String eventoId, String laboratorio, String dispositivoId,
                        Long alunoId, TipoEventoIot tipo, Instant ocorridoEm) {

    public static final int TAMANHO_MAXIMO_ID = 64;

    public EventoIot {
        exigirTexto(eventoId, "eventoId");
        exigirTexto(laboratorio, "laboratorio");
        exigirTexto(dispositivoId, "dispositivoId");
        if (eventoId.length() > TAMANHO_MAXIMO_ID) {
            throw new IllegalArgumentException("eventoId com mais de " + TAMANHO_MAXIMO_ID + " caracteres");
        }
        if (alunoId == null || alunoId <= 0) {
            throw new IllegalArgumentException("alunoId inválido");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo é obrigatório");
        }
        if (ocorridoEm == null) {
            throw new IllegalArgumentException("ocorridoEm é obrigatório");
        }
    }

    public boolean concluiCurso() {
        return tipo.concluiCurso();
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
    }
}