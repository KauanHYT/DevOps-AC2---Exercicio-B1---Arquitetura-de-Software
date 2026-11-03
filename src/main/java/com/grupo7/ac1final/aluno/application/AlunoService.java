package com.grupo7.ac1final.aluno.application;

import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoAlunoConsulta;
import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.domain.Aluno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;

import java.util.List;
import java.util.Optional;

/**
 * Casos de uso do módulo aluno. Orquestra: busca pela porta, delega a regra ao
 * domínio e salva pela porta. Não conhece JPA, entidades nem HTTP (ADR-002).
 */
@Service
public class AlunoService implements ProgressoAlunoConsulta, ProgressoIndividualConsulta {

    private final AlunoRepositoryPort repositorio;

    public AlunoService(AlunoRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Aluno cadastrar(DadosCadastroAluno dados) {
        String plano = (dados.plano() == null || dados.plano().isBlank()) ? "BASICO" : dados.plano();
        Aluno novo = Aluno.reconstituir(null, plano, dados.cursosConcluidos(),
                dados.cursosAdicionaisPermitidos(), dados.possuiVoucher(), dados.notificadoMelhoriaPlano());
        return repositorio.salvar(novo);
    }

    public List<Aluno> listarTodos() {
        return repositorio.listarTodos();
    }

    public Optional<Aluno> buscarPorId(Long id) {
        return repositorio.buscarPorId(id);
    }

    /** @return false se o aluno não existir */
    @Transactional
    public boolean remover(Long id) {
        if (!repositorio.existe(id)) {
            return false;
        }
        repositorio.remover(id);
        return true;
    }

    /** US 2: ao concluir 12 cursos o aluno vira PREMIUM e ganha voucher (regra no domínio). */
    @Transactional
    public Optional<Aluno> concluirCurso(Long id) {
        return repositorio.buscarPorId(id).map(aluno -> {
            aluno.concluirCurso();
            return repositorio.salvar(aluno);
        });
    }

    /** US 1 e 4: 3 cursos extras somente com média maior ou igual a 7,0 (regra no domínio). */
    @Transactional
    public Optional<Aluno> liberarCursosExtras(Long id, double media) {
        return repositorio.buscarPorId(id).map(aluno -> {
            aluno.concluirCurso(media);
            return repositorio.salvar(aluno);
        });
    }

    /** US 3: registra a notificação somente se o aluno já for PREMIUM (regra no domínio). */
    @Transactional
    public Optional<Aluno> registrarNotificacaoMudancaPlano(Long id) {
        return repositorio.buscarPorId(id).map(aluno -> {
            aluno.identificarMudancaDePlano();
            return repositorio.salvar(aluno);
        });
    }

    @Override
    public Optional<ProgressoAluno> buscarProgresso(Long alunoId) {
        return repositorio.buscarPorId(alunoId)
                .map(aluno -> new ProgressoAluno(aluno.getId(), aluno.getPlano(), aluno.getCursosConcluidos()));
    }

    @Override
    public List<ProgressoAluno> listarMaioresProgressos(int limite) {
        return repositorio.buscarMaioresProgressos(limite)
                .stream()
                .map(aluno -> new ProgressoAluno(aluno.getId(), aluno.getPlano(), aluno.getCursosConcluidos()))
                .toList();
    }
}
