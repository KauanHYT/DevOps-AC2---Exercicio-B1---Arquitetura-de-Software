package com.grupo7.ac1final.iot.adapter.web;

import com.grupo7.ac1final.iot.domain.EventoIot;
import com.grupo7.ac1final.iot.domain.TipoEventoIot;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Contrato HTTP entre o iot-gateway e o Core (formato da plataforma, não do dispositivo).
 * Tipo e data chegam como texto para que um valor inválido vire 400 com mensagem clara.
 */
public record EventoIotDTO(String eventoId, String laboratorio, String dispositivoId,
                           Long alunoId, String tipo, String ocorridoEm) {

    public EventoIot paraDominio() {
        if (tipo == null || ocorridoEm == null) {
            throw new IllegalArgumentException("tipo e ocorridoEm são obrigatórios");
        }
        TipoEventoIot tipoEvento = TipoEventoIot.valueOf(tipo);
        Instant quando;
        try {
            quando = Instant.parse(ocorridoEm);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("ocorridoEm inválido: " + ocorridoEm);
        }
        return new EventoIot(eventoId, laboratorio, dispositivoId, alunoId, tipoEvento, quando);
    }

    public static EventoIotDTO de(EventoIot evento) {
        return new EventoIotDTO(evento.eventoId(), evento.laboratorio(), evento.dispositivoId(),
                evento.alunoId(), evento.tipo().name(), evento.ocorridoEm().toString());
    }
}
