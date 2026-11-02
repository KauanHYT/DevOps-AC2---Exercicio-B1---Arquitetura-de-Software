package com.grupo7.ac1final.aluno.adapter.persistence;

import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPortContrato;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Mesmo contrato da porta, agora contra o adapter JPA real (H2 em memória). */
@DataJpaTest
@Import(AlunoPersistenceAdapter.class)
class AlunoPersistenceAdapterJpaTest extends AlunoRepositoryPortContrato {

    @Autowired
    private AlunoPersistenceAdapter adapter;

    @Override
    protected AlunoRepositoryPort repositorio() {
        return adapter;
    }
}
