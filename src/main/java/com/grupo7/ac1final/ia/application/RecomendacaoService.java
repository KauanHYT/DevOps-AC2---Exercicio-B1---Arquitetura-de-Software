package com.grupo7.ac1final.ia.application;

import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.application.port.RecomendacaoIaPort;
import com.grupo7.ac1final.ia.domain.OrigemRecomendacao;
import com.grupo7.ac1final.ia.domain.RecomendacaoDoModelo;
import com.grupo7.ac1final.ia.domain.RecomendacaoPadrao;
import com.grupo7.ac1final.ia.domain.Recomendacoes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * RF03. Busca o progresso pela API do módulo aluno, pede a recomendação ao modelo
 * e, se a IA estiver indisponível, degrada para a trilha padrão em vez de falhar (ADR-003).
 */
@Service
public class RecomendacaoService {

    static final int LIMITE_MAXIMO = 10;

    private static final Logger log = LoggerFactory.getLogger(RecomendacaoService.class);

    private final ProgressoIndividualConsulta progressoAlunos;
    private final RecomendacaoIaPort modelo;

    public RecomendacaoService(ProgressoIndividualConsulta progressoAlunos, RecomendacaoIaPort modelo) {
        this.progressoAlunos = progressoAlunos;
        this.modelo = modelo;
    }

    /** @return vazio se o aluno não existir */
    public Optional<Recomendacoes> recomendar(Long alunoId, int limite) {
        int limiteValido = Math.max(1, Math.min(limite, LIMITE_MAXIMO));

        return progressoAlunos.buscarProgresso(alunoId).map(progresso -> {
            try {
                RecomendacaoDoModelo resposta = modelo.recomendar(progresso, limiteValido);
                return new Recomendacoes(alunoId, OrigemRecomendacao.IA, resposta.modeloVersao(), resposta.itens());
            } catch (IaIndisponivelException e) {
                log.warn("IA indisponível para o aluno {}: usando a trilha padrão ({})", alunoId, e.getMessage());
                return RecomendacaoPadrao.para(alunoId, limiteValido);
            }
        });
    }
}