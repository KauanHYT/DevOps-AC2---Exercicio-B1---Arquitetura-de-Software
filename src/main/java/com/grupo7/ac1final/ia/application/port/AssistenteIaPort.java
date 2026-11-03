package com.grupo7.ac1final.ia.application.port;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.ia.domain.RespostaAssistente;

/** Porta de saída para o assistente educacional RAG/LLM (RF04). */
public interface AssistenteIaPort {

    /** @throws IaIndisponivelException se o assistente não responder dentro do tempo limite */
    RespostaAssistente perguntar(ProgressoAluno contexto, String pergunta);
}