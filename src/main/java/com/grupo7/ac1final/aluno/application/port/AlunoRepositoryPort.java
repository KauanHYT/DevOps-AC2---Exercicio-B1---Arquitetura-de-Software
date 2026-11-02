package com.grupo7.ac1final.aluno.application.port;

import com.grupo7.ac1final.aluno.domain.Aluno;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída (Ports &amp; Adapters, ADR-002): o que a aplicação precisa da persistência,
 * descrito em termos do domínio. Implementações: AlunoPersistenceAdapter (JPA, produção)
 * e AlunoRepositoryEmMemoria (testes).
 */
public interface AlunoRepositoryPort {

    /** Insere (id nulo) ou atualiza o aluno e devolve o estado persistido, já com id. */
    Aluno salvar(Aluno aluno);

    Optional<Aluno> buscarPorId(Long id);

    List<Aluno> listarTodos();

    boolean existe(Long id);

    void remover(Long id);

    /** Alunos com mais cursos concluídos (desc); empate pelo menor id. */
    List<Aluno> buscarMaioresProgressos(int limite);
}
