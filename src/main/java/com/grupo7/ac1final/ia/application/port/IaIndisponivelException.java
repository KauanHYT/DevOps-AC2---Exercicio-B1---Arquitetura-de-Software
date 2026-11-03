package com.grupo7.ac1final.ia.application.port;

/**
 * O serviço de IA não respondeu a tempo, recusou a conexão ou devolveu erro.
 * Os adapters traduzem qualquer falha técnica para esta exceção, e a aplicação
 * decide o comportamento degradado (ADR-003).
 */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}