package com.grupo7.ac1final.ranking;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoAlunoConsulta;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Módulo ranking (RF01). Conhece apenas a API pública do módulo aluno,
 * o que permite extraí-lo para um serviço próprio no futuro sem tocar no aluno.
 */
@Service
public class RankingService {

    static final int LIMITE_MAXIMO = 100;

    private final ProgressoAlunoConsulta progressoAlunos;

    public RankingService(ProgressoAlunoConsulta progressoAlunos) {
        this.progressoAlunos = progressoAlunos;
    }

    public List<PosicaoRanking> topAlunos(int limite) {
        int limiteValido = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        List<ProgressoAluno> progressos = progressoAlunos.listarMaioresProgressos(limiteValido);

        List<PosicaoRanking> ranking = new ArrayList<>();
        for (int i = 0; i < progressos.size(); i++) {
            ProgressoAluno p = progressos.get(i);
            ranking.add(new PosicaoRanking(i + 1, p.alunoId(), p.plano(), p.cursosConcluidos()));
        }
        return ranking;
    }
}
