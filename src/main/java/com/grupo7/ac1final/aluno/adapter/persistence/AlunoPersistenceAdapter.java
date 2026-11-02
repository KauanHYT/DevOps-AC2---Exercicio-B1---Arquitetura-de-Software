package com.grupo7.ac1final.aluno.adapter.persistence;

import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.domain.Aluno;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter de saída JPA (ADR-002): implementa a porta do domínio usando Spring Data JPA
 * e faz a tradução entidade JPA &lt;-&gt; domínio. Trocar de banco ou ORM afeta só este pacote.
 */
@Component
public class AlunoPersistenceAdapter implements AlunoRepositoryPort {

    private final AlunoJpaRepository jpa;

    public AlunoPersistenceAdapter(AlunoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Aluno salvar(Aluno aluno) {
        return paraDominio(jpa.save(paraEntidade(aluno)));
    }

    @Override
    public Optional<Aluno> buscarPorId(Long id) {
        return jpa.findById(id).map(AlunoPersistenceAdapter::paraDominio);
    }

    @Override
    public List<Aluno> listarTodos() {
        return jpa.findAll().stream().map(AlunoPersistenceAdapter::paraDominio).toList();
    }

    @Override
    public boolean existe(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public void remover(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Aluno> buscarMaioresProgressos(int limite) {
        return jpa.findAllByOrderByCursosConcluidosDescIdAsc(PageRequest.of(0, limite))
                .stream()
                .map(AlunoPersistenceAdapter::paraDominio)
                .toList();
    }

    private static AlunoEntity paraEntidade(Aluno aluno) {
        AlunoEntity entidade = new AlunoEntity();
        entidade.setId(aluno.getId());
        entidade.setPlano(aluno.getPlano());
        entidade.setCursosConcluidos(aluno.getCursosConcluidos());
        entidade.setCursosAdicionaisPermitidos(aluno.getCursosAdicionaisPermitidos());
        entidade.setPossuiVoucher(aluno.isPossuiVoucher());
        entidade.setNotificadoMelhoriaPlano(aluno.isNotificadoMelhoriaPlano());
        return entidade;
    }

    private static Aluno paraDominio(AlunoEntity entidade) {
        return Aluno.reconstituir(
                entidade.getId(),
                entidade.getPlano(),
                entidade.getCursosConcluidos(),
                entidade.getCursosAdicionaisPermitidos(),
                entidade.isPossuiVoucher(),
                entidade.isNotificadoMelhoriaPlano());
    }
}
