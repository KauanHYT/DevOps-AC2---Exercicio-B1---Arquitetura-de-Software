package com.grupo7.ac1final.ia.application;

import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.ia.application.port.AssistenteIaPort;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * RF04. Sem fallback de conteúdo: se o assistente cair, a resposta é "indisponível"
 * (HTTP 503) em vez de uma resposta inventada. Nenhuma outra função da plataforma
 * depende dele (ADR-003).
 */
@Service
public class AssistenteService {

    private final ProgressoIndividualConsulta progressoAlunos;
    private final AssistenteIaPort assistente;

    public AssistenteService(ProgressoIndividualConsulta progressoAlunos, AssistenteIaPort assistente) {
        this.progressoAlunos = progressoAlunos;
        this.assistente = assistente;
    }

    /**
     * @return vazio se o aluno não existir
     * @throws com.grupo7.ac1final.ia.application.port.IaIndisponivelException se o assistente estiver fora
     */
    public Optional<RespostaAssistente> perguntar(Long alunoId, String pergunta) {
        return progressoAlunos.buscarProgresso(alunoId)
                .map(contexto -> assistente.perguntar(contexto, pergunta));
    }
}