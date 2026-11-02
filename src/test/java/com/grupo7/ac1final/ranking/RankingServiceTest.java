package com.grupo7.ac1final.ranking;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoAlunoConsulta;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O ranking é testado sem banco e sem Spring: basta uma implementação falsa
 * da API pública do módulo aluno (evidência do Desafio 1).
 */
class RankingServiceTest {

    private final List<Integer> limitesSolicitados = new ArrayList<>();

    private final ProgressoAlunoConsulta consultaFalsa = limite -> {
        limitesSolicitados.add(limite);
        return List.of(
                new ProgressoAluno(7L, "PREMIUM", 12),
                new ProgressoAluno(3L, "BASICO", 5),
                new ProgressoAluno(9L, "BASICO", 1));
    };

    private final RankingService rankingService = new RankingService(consultaFalsa);

    @Test
    void deveNumerarPosicoesNaOrdemRecebidaDoModuloAluno() {
        List<PosicaoRanking> ranking = rankingService.topAlunos(10);

        assertEquals(3, ranking.size());
        assertEquals(new PosicaoRanking(1, 7L, "PREMIUM", 12), ranking.get(0));
        assertEquals(new PosicaoRanking(2, 3L, "BASICO", 5), ranking.get(1));
        assertEquals(new PosicaoRanking(3, 9L, "BASICO", 1), ranking.get(2));
    }

    @Test
    void deveLimitarQuantidadeSolicitadaEntreUmECem() {
        rankingService.topAlunos(0);
        rankingService.topAlunos(5000);

        assertEquals(List.of(1, RankingService.LIMITE_MAXIMO), limitesSolicitados);
    }
}
