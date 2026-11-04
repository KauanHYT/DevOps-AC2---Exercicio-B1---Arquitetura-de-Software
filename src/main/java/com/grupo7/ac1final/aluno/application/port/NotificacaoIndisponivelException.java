package com.grupo7.ac1final.aluno.application.port;

public class NotificacaoIndisponivelException extends RuntimeException {

    public NotificacaoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}