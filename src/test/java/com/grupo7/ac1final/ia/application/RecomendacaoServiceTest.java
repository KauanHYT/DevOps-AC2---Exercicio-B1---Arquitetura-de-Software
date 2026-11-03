package com.grupo7.ac1final.ia.application;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.ia.application.port.IaIndisponivelException;
import com.grupo7.ac1final.ia.application.port.RecomendacaoIaPort;
import com.grupo7.ac1final.ia.domain.ItemRecomendado;
import com.grupo7.ac1final.ia.domain.OrigemRecomendacao;
import com.grupo7.ac1final.ia.domain.RecomendacaoDoModelo;
import com.grupo7.ac1final.ia.domain.RecomendacaoPadrao;
import com.grupo7.ac1final.ia.domain.Recomendacoes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Casos de uso de recomendação com portas falsas: sem Spring, sem HTTP, sem Python. */
class RecomendacaoServiceTest {

    private static final ProgressoAluno ALUNO_1 = new ProgressoAluno(1L, "BASICO", 5);

    private final ProgressoIndividualConsulta consulta =
            id -> id == 1L ? Optional.of(ALUNO_1) : Optional.empty();

    private final List<Integer> limitesPedidos = new ArrayList<>();

    private final RecomendacaoIaPort modeloNoAr = (progresso, limite) -> {
        limitesPedidos.add(limite);
        return new RecomendacaoDoModelo("regras-v1",
                List.of(new ItemRecomendado("WEB-210", "APIs REST com Spring Boot", "Próximo passo")));
    };

    private final RecomendacaoIaPort modeloFora = (progresso, limite) -> {
        throw new IaIndisponivelException("connection refused");
    };

    @Test
    void comIaNoArARecomendacaoVemDoModelo() {
        Recomendacoes r = new RecomendacaoService(consulta, modeloNoAr).recomendar(1L, 3).orElseThrow();

        assertEquals(OrigemRecomendacao.IA, r.origem());
        assertEquals("regras-v1", r.modeloVersao());
        assertEquals("WEB-210", r.itens().get(0).cursoId());
    }

    @Test
    void comIaForaDoArDegradaParaATrilhaPadraoEmVezDeFalhar() {
        Recomendacoes r = new RecomendacaoService(consulta, modeloFora).recomendar(1L, 3).orElseThrow();

        assertEquals(OrigemRecomendacao.PADRAO, r.origem());
        assertEquals(RecomendacaoPadrao.VERSAO, r.modeloVersao());
        assertEquals(3, r.itens().size());
    }

    @Test
    void alunoInexistenteNaoChamaOModelo() {
        assertTrue(new RecomendacaoService(consulta, modeloNoAr).recomendar(99L, 3).isEmpty());
        assertTrue(limitesPedidos.isEmpty());
    }

    @Test
    void limiteFicaEntreUmEDez() {
        RecomendacaoService service = new RecomendacaoService(consulta, modeloNoAr);

        service.recomendar(1L, 0);
        service.recomendar(1L, 500);

        assertEquals(List.of(1, RecomendacaoService.LIMITE_MAXIMO), limitesPedidos);
    }
}