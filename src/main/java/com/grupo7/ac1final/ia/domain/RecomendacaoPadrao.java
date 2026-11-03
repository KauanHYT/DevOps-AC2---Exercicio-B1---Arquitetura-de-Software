package com.grupo7.ac1final.ia.domain;

import java.util.List;

/**
 * Comportamento degradado aceito no ADR-003: quando o serviço de IA não responde a tempo,
 * o Core devolve uma trilha introdutória fixa, marcada como origem PADRAO.
 */
public final class RecomendacaoPadrao {

    public static final String VERSAO = "padrao-core-v1";

    private static final String MOTIVO = "Trilha padrão: a recomendação personalizada está indisponível no momento.";

    private static final List<ItemRecomendado> TRILHA_INICIAL = List.of(
            new ItemRecomendado("FUND-101", "Fundamentos de Programação", MOTIVO),
            new ItemRecomendado("FUND-102", "Lógica e Algoritmos", MOTIVO),
            new ItemRecomendado("WEB-110", "HTML, CSS e JavaScript", MOTIVO));

    private RecomendacaoPadrao() {
    }

    public static Recomendacoes para(Long alunoId, int limite) {
        int quantidade = Math.max(1, Math.min(limite, TRILHA_INICIAL.size()));
        return new Recomendacoes(alunoId, OrigemRecomendacao.PADRAO, VERSAO, TRILHA_INICIAL.subList(0, quantidade));
    }
}