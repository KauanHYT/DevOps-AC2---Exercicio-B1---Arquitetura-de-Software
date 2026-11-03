package com.grupo7.ac1final.ia.application.port;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.ia.domain.RecomendacaoDoModelo;

/** Porta de saída para o modelo de recomendação (RF03). */
public interface RecomendacaoIaPort {

    /** @throws IaIndisponivelException se o modelo não responder dentro do tempo limite */
    RecomendacaoDoModelo recomendar(ProgressoAluno progresso, int limite);
}