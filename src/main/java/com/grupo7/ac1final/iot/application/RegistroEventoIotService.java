package com.grupo7.ac1final.iot.application;

import com.grupo7.ac1final.aluno.api.ConclusaoDeCurso;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.iot.application.port.EventoIotRepositoryPort;
import com.grupo7.ac1final.iot.domain.EventoIot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso do RF06: recebe um evento IoT já traduzido pelo gateway (ADR-005).
 *
 * A entrega pelo MQTT é "pelo menos uma vez": o mesmo evento pode chegar mais de uma vez.
 * Por isso o efeito (concluir o curso) e o registro do `eventoId` acontecem na MESMA
 * transação. Ou os dois ficam gravados, ou nenhum, e um evento repetido nunca conta duas vezes.
 */
@Service
public class RegistroEventoIotService {

    public enum Resultado { REGISTRADO, DUPLICADO, ALUNO_INEXISTENTE }

    private final EventoIotRepositoryPort eventos;
    private final ConclusaoDeCurso conclusaoDeCurso;
    private final ProgressoIndividualConsulta progresso;

    public RegistroEventoIotService(EventoIotRepositoryPort eventos,
                                    ConclusaoDeCurso conclusaoDeCurso,
                                    ProgressoIndividualConsulta progresso) {
        this.eventos = eventos;
        this.conclusaoDeCurso = conclusaoDeCurso;
        this.progresso = progresso;
    }

    @Transactional
    public Resultado registrar(EventoIot evento) {
        if (eventos.jaRegistrado(evento.eventoId())) {
            return Resultado.DUPLICADO;
        }
        boolean alunoExiste = evento.concluiCurso()
                ? conclusaoDeCurso.concluirCursoDoAluno(evento.alunoId()).isPresent()
                : progresso.buscarProgresso(evento.alunoId()).isPresent();
        if (!alunoExiste) {
            return Resultado.ALUNO_INEXISTENTE;
        }
        eventos.registrar(evento);
        return Resultado.REGISTRADO;
    }

    public List<EventoIot> eventosDoAluno(Long alunoId) {
        return eventos.listarDoAluno(alunoId);
    }
}
