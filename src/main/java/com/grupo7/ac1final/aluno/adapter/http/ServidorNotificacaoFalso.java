package com.grupo7.ac1final.aluno.adapter.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/** notificacao-service falso (HttpServer do JDK): pode responder 201 ou estar "fora do ar" (503). */
public class ServidorNotificacaoFalso {

    private final HttpServer servidor;
    private volatile boolean noAr = true;
    private final AtomicInteger recebidas = new AtomicInteger();

    public ServidorNotificacaoFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        servidor.createContext("/notificacoes", troca -> {
            troca.getRequestBody().readAllBytes();
            int status = noAr ? 201 : 503;
            if (noAr) {
                recebidas.incrementAndGet();
            }
            byte[] corpo = "{}".getBytes();
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public void ficarForaDoAr() {
        noAr = false;
    }

    public void voltar() {
        noAr = true;
    }

    public int recebidas() {
        return recebidas.get();
    }
}