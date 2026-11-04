package com.grupo7.ac1final.iot.adapter.web;

import com.grupo7.ac1final.iot.application.RegistroEventoIotService;
import com.grupo7.ac1final.iot.application.RegistroEventoIotService.Resultado;
import com.grupo7.ac1final.iot.domain.EventoIot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Porta de entrada dos eventos IoT (RF06). Quem chama é o iot-gateway, nunca o dispositivo:
 * o Core não conhece MQTT nem o formato de cada aparelho (ADR-005).
 *   201 REGISTRADO | 200 DUPLICADO (retransmissão, nada muda) | 404 aluno inexistente | 400 inválido
 */
@RestController
@RequestMapping("/iot/eventos")
public class EventoIotController {

    private final RegistroEventoIotService servico;

    public EventoIotController(RegistroEventoIotService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receber(@RequestBody EventoIotDTO dto) {
        EventoIot evento = dto.paraDominio();
        Resultado resultado = servico.registrar(evento);
        Map<String, String> corpo = Map.of("eventoId", evento.eventoId(), "resultado", resultado.name());
        return switch (resultado) {
            case REGISTRADO -> ResponseEntity.status(201).body(corpo);
            case DUPLICADO -> ResponseEntity.ok(corpo);
            case ALUNO_INEXISTENTE -> ResponseEntity.status(404).body(corpo);
        };
    }

    @GetMapping
    public List<EventoIotDTO> listar(@RequestParam("alunoId") Long alunoId) {
        return servico.eventosDoAluno(alunoId).stream().map(EventoIotDTO::de).toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> eventoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", String.valueOf(e.getMessage())));
    }
}
