package com.grupo7.ac1final.ia.adapter.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * ia-service falso, feito com o HttpServer do próprio JDK, para testar o adapter HTTP
 * sem Python: responde o contrato real, pode ficar lento, devolver erro ou ser desligado.
 */
public class ServidorIaFalso implements AutoCloseable {

    public static final String JSON_RECOMENDACAO = """
            {"alunoId":1,"modeloVersao":"regras-v1","campoNovoDoPython":true,
             "itens":[{"cursoId":"WEB-210","titulo":"APIs REST com Spring Boot","motivo":"Próximo passo"}]}""";

    public static final String JSON_ASSISTENTE = """
            {"resposta":"Ao concluir 12 cursos você vira PREMIUM.","fontes":["regra-premium"],"modeloVersao":"rag-simulado-v1"}""";

    private final HttpServer servidor;
    private volatile long atrasoMs = 0;
    private volatile int status = 200;

    public ServidorIaFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        servidor.createContext("/recomendacoes", troca -> responder(troca, JSON_RECOMENDACAO));
        servidor.createContext("/assistente/perguntas", troca -> responder(troca, JSON_ASSISTENTE));
        servidor.start();
    }

    private void responder(com.sun.net.httpserver.HttpExchange troca, String json) throws IOException {
        try {
            if (atrasoMs > 0) {
                Thread.sleep(atrasoMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        troca.getRequestBody().readAllBytes();
        byte[] corpo = (status == 200 ? json : "{\"erro\":\"falha interna\"}").getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "application/json");
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public void ficarLento(long ms) {
        this.atrasoMs = ms;
    }

    public void responderComErro(int statusHttp) {
        this.status = statusHttp;
    }

    public void desligar() {
        servidor.stop(0);
    }

    @Override
    public void close() {
        desligar();
    }
}