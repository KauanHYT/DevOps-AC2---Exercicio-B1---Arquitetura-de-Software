package com.grupo7.ac1final.aluno.application;

import com.grupo7.ac1final.aluno.api.ConclusaoDeCurso;
import com.grupo7.ac1final.aluno.api.ProgressoAluno;
import com.grupo7.ac1final.aluno.api.ProgressoAlunoConsulta;
import com.grupo7.ac1final.aluno.api.ProgressoIndividualConsulta;
import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.application.port.NotificadorPromocao;
import com.grupo7.ac1final.aluno.domain.Aluno;
import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Service
public class AlunoService implements ProgressoAlunoConsulta, ProgressoIndividualConsulta, ConclusaoDeCurso {

    private final AlunoRepositoryPort repositorio;
    private final NotificadorPromocao notificador;

    public AlunoService(AlunoRepositoryPort repositorio, NotificadorPromocao notificador) {
        this.repositorio = repositorio;
        this.notificador = notificador;
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

    @Transactional
    public boolean remover(Long id) {
        if (!repositorio.existe(id)) {
            return false;
        }
        repositorio.remover(id);
        return true;
    }

    @Transactional
    public Optional<Aluno> concluirCurso(Long id) {
        return repositorio.buscarPorId(id).map(this::concluirCursoDe);
    }

    private Aluno concluirCursoDe(Aluno aluno) {
        boolean eraPremium = aluno.isPremium();
        aluno.concluirCurso();
        Aluno salvo = repositorio.salvar(aluno);

        if (eraPremium || !salvo.isPremium()) {
            return salvo;
        }
        AlunoPromovidoPremium evento = AlunoPromovidoPremium.de(salvo, Instant.now());
        if (notificador.notificar(evento) == NotificadorPromocao.Resultado.ENTREGUE) {
            salvo.identificarMudancaDePlano();
            salvo = repositorio.salvar(salvo);
        }
        return salvo;
    }

    @Transactional
    public Optional<Aluno> liberarCursosExtras(Long id, double media) {
        return repositorio.buscarPorId(id).map(aluno -> {
            aluno.concluirCurso(media);
            return repositorio.salvar(aluno);
        });
    }

    @Transactional
    public Optional<Aluno> registrarNotificacaoMudancaPlano(Long id) {
        return repositorio.buscarPorId(id).map(aluno -> {
            aluno.identificarMudancaDePlano();
            return repositorio.salvar(aluno);
        });
    }

    /** API pública usada pelo módulo iot (Desafio 5): mesma regra do PUT /concluir-curso. */
    @Override
    @Transactional
    public Optional<ProgressoAluno> concluirCursoDoAluno(Long alunoId) {
        return concluirCurso(alunoId).map(AlunoService::progressoDe);
    }

    @Override
    public Optional<ProgressoAluno> buscarProgresso(Long alunoId) {
        return repositorio.buscarPorId(alunoId).map(AlunoService::progressoDe);
    }

    @Override
    public List<ProgressoAluno> listarMaioresProgressos(int limite) {
        return repositorio.buscarMaioresProgressos(limite)
                .stream()
                .map(AlunoService::progressoDe)
                .toList();
    }

    private static ProgressoAluno progressoDe(Aluno aluno) {
        return new ProgressoAluno(aluno.getId(), aluno.getPlano(), aluno.getCursosConcluidos());
    }
}