package com.grupo7.ac1final.ia.domain;

import java.util.List;

/** O que o modelo de IA devolveu, já traduzido para o vocabulário do Core. */
public record RecomendacaoDoModelo(String modeloVersao, List<ItemRecomendado> itens) {
}