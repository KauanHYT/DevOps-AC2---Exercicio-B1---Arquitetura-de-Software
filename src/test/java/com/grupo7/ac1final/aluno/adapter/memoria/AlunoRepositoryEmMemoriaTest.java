package com.grupo7.ac1final.aluno.adapter.memoria;

import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPort;
import com.grupo7.ac1final.aluno.application.port.AlunoRepositoryPortContrato;

/** Contrato da porta executado contra o adapter em memória (sem Spring, sem banco). */
class AlunoRepositoryEmMemoriaTest extends AlunoRepositoryPortContrato {

    private final AlunoRepositoryEmMemoria repositorio = new AlunoRepositoryEmMemoria();

    @Override
    protected AlunoRepositoryPort repositorio() {
        return repositorio;
    }
}
