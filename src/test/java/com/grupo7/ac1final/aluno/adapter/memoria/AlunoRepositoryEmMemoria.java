package com.grupo7.ac1final.aluno.adapter.memoria;

import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.domain.Aluno;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adapter fake da porta de persistência (ADR-002). Guarda cópias em memória,
 * como um banco faria, para testar casos de uso sem Spring e sem banco.
 */
public class AlunoRepositoryEmMemoria implements AlunoRepositoryPort {

    private final Map<Long, Aluno> dados = new LinkedHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override
    public Aluno salvar(Aluno aluno) {
        Long id = aluno.getId() != null ? aluno.getId() : sequencia.incrementAndGet();
        Aluno copia = copiar(aluno, id);
        dados.put(id, copia);
        return copiar(copia, id);
    }

    @Override
    public Optional<Aluno> buscarPorId(Long id) {
        return Optional.ofNullable(dados.get(id)).map(a -> copiar(a, id));
    }

    @Override
    public List<Aluno> listarTodos() {
        List<Aluno> todos = new ArrayList<>();
        dados.forEach((id, a) -> todos.add(copiar(a, id)));
        return todos;
    }

    @Override
    public boolean existe(Long id) {
        return dados.containsKey(id);
    }

    @Override
    public void remover(Long id) {
        dados.remove(id);
    }

    @Override
    public List<Aluno> buscarMaioresProgressos(int limite) {
        return listarTodos().stream()
                .sorted(Comparator.comparingInt(Aluno::getCursosConcluidos).reversed()
                        .thenComparing(Aluno::getId))
                .limit(limite)
                .toList();
    }

    private static Aluno copiar(Aluno a, Long id) {
        return Aluno.reconstituir(id, a.getPlano(), a.getCursosConcluidos(),
                a.getCursosAdicionaisPermitidos(), a.isPossuiVoucher(), a.isNotificadoMelhoriaPlano());
    }
}
