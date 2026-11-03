package com.grupo7.ac1final.ia.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecomendacaoPadraoTest {

    @Test
    void trilhaPadraoEMarcadaComoOrigemPadrao() {
        Recomendacoes r = RecomendacaoPadrao.para(7L, 3);

        assertEquals(7L, r.alunoId());
        assertEquals(OrigemRecomendacao.PADRAO, r.origem());
        assertEquals(3, r.itens().size());
    }

    @Test
    void respeitaOLimiteSemPassarDoTamanhoDaTrilha() {
        assertEquals(1, RecomendacaoPadrao.para(1L, 1).itens().size());
        assertEquals(3, RecomendacaoPadrao.para(1L, 10).itens().size());
    }
}