# AC1 Final - Educação Continuada Gamificada

## Descrição do estudo de caso

O estudo de caso desenvolvido nesta atividade é o **Educação Continuada Gamificada**, apresentado em aula.

A proposta consiste no desenvolvimento de uma aplicação relacionada à educação continuada, utilizando práticas de desenvolvimento orientadas a testes (TDD), uma API REST com Spring Boot, persistência em PostgreSQL e uma interface web em VueJS.

> **B1 — Architecture Lab:** as decisões arquiteturais (ASRs, ADRs, C4 e evidências) estão em [`docs/arquitetura/README.md`](docs/arquitetura/README.md).

---

## User Stories (US)

Cada integrante do grupo ficou responsável por uma User Story.

| # | Integrante responsável | User Story |
|---|---|---|
| 1 | Guilherme Luiz Ifanger | COMO aluno assinante do plano básico, QUERO concluir um curso com média ≥ 7,0, PARA ter direito a realizar mais 3 cursos |
| 2 | Kauan Freitas | COMO aluno que atingiu 12 cursos concluídos, QUERO ter meu plano atualizado automaticamente para "Premium", PARA receber voucher de participação em projetos reais |
| 3 | Giovane Zanzarini | COMO aluno da plataforma, QUERO ser notificado quando meu plano mudar para "Premium", PARA saber que já posso resgatar os vouchers e as moedas |
| 4 | Enrico Affonso | COMO aluno assinante do plano básico, QUERO ter acesso liberado automaticamente aos 3 cursos extras ao atingir média ≥ 7,0, PARA continuar estudando sem depender de aprovação manual |

## User Story escolhida

A User Story escolhida para desenvolvimento foi a **US 2**, relacionada à **conclusão de 12 cursos pelo aluno**.

A regra implementada determina que, ao concluir 12 cursos:

- O plano do aluno passa de **BASICO** para **PREMIUM**;
- O aluno passa a possuir um **voucher**.

---

## BDDs escritos pelos integrantes

### Kauan Freitas (US 2)

BDDs relacionados à User Story escolhida:

- Alterar o plano do aluno para PREMIUM ao concluir 12 cursos.
- Conceder um voucher ao aluno ao concluir 12 cursos.

Testes implementados:

```java
deveAlterarPlanoParaPremiumAoConcluir12Cursos()
```

```java
deveReceberVoucherAoConcluir12Cursos()
```

### Guilherme Luiz Ifanger (US 1)

BDD relacionado à User Story:

- Conceder ao aluno o direito a mais 3 cursos quando concluir um curso com média maior ou igual a 7.

Teste implementado:

```java
deveGanharDireitoAMaisTresCursosQuandoAlunoConcluirCursoComMediaMaiorOuIgualA7()
```

### Giovane Zanzarini (US 3)

BDD relacionado à User Story:

- Receber notificação de melhoria de plano quando a aplicação identificar a mudança para PREMIUM.

Teste implementado:

```java
deveReceberNotificacaoDeMelhoriaDePlanoQuandoAplicacaoIdentificarMudancaParaPremium()
```

### Enrico Affonso (US 4)

BDD relacionado à User Story:

- Liberar automaticamente 3 cursos extras para o aluno do plano básico com média maior ou igual a 7.

Teste implementado:

```java
deveLiberarTresCursosExtrasParaAlunoDoPlanoBasicoComMediaMaiorOuIgualA7()
```

---

# Tecnologias e ferramentas utilizadas

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring Web MVC**
- **Spring Data JPA**
- **Maven**
- **JUnit 5**
- **JaCoCo**
- **H2**
- **PostgreSQL 17**
- **PGAdmin 4**
- **Swagger / OpenAPI**
- **VueJS**
- **Node.js / npm**
- **Docker**
- **Docker Compose**
- **Nginx**

---

# Estrutura do projeto

A aplicação é composta por um backend em Spring Boot e um frontend em VueJS. Desde o B1, o backend é um **monólito modular** (ADR-001) e o módulo `aluno` segue **Ports & Adapters** (ADR-002).

```text
ac1final/
├── src/
│   ├── main/java/com/grupo7/ac1final/
│   │   ├── aluno/                          ← módulo aluno
│   │   │   ├── api/                        ← API pública do módulo (usada pelo ranking)
│   │   │   ├── domain/Aluno.java           ← regras US 1–4 (Java puro)
│   │   │   ├── application/                ← casos de uso + porta AlunoRepositoryPort
│   │   │   └── adapter/
│   │   │       ├── web/                    ← AlunoController, AlunoDTO
│   │   │       └── persistence/            ← AlunoEntity, AlunoJpaRepository, AlunoPersistenceAdapter
│   │   └── ranking/                        ← módulo ranking (GET /ranking)
│   └── test/java/com/grupo7/ac1final/
│       ├── aluno/                          ← domínio, casos de uso, contrato da porta, integração
│       ├── ranking/
│       └── arquitetura/                    ← regras ArchUnit (ADR-001 e ADR-002)
├── docs/arquitetura/                       ← registro arquitetural do B1
├── frontend/
├── Dockerfile
├── docker-compose.yaml
├── pom.xml
└── README.md
```

---

# Dependências do Spring Boot

O projeto utiliza:

- Spring Web
- Spring Data JPA
- H2
- PostgreSQL
- PGAdmin para administração do banco via container
- SpringDoc OpenAPI para Swagger
- JaCoCo para análise da cobertura dos testes

---

# TDD

## Domain

Foi criado o pacote `Domain` contendo:

```text
src/main/java/com/grupo7/ac1final/Domain/Aluno.java
```

A classe `Aluno` representa o domínio do aluno e contém as regras de negócio relacionadas às User Stories.

## DomainTest

Foi criado o pacote `DomainTest`:

```text
src/test/java/com/grupo7/ac1final/DomainTest/AlunoTest.java
```

## RED

Na etapa **RED**, foram criados os testes referentes aos comportamentos esperados antes da implementação completa das regras de negócio.

Para a US 2:

```java
@Test
void deveAlterarPlanoParaPremiumAoConcluir12Cursos() {
    Aluno aluno = new Aluno();

    for (int i = 0; i < 12; i++) {
        aluno.concluirCurso();
    }

    assertEquals("PREMIUM", aluno.getPlano());
}
```

```java
@Test
void deveReceberVoucherAoConcluir12Cursos() {
    Aluno aluno = new Aluno();

    for (int i = 0; i < 12; i++) {
        aluno.concluirCurso();
    }

    assertTrue(aluno.isPossuiVoucher());
}
```

Nesta etapa, os testes foram executados antes da implementação das regras necessárias, apresentando falhas.

## GREEN

Na etapa **GREEN**, foram implementadas as regras necessárias:

```java
if (cursosConcluidos >= 12) {
    plano = "PREMIUM";
    possuiVoucher = true;
}
```

Após a implementação, os testes passaram com sucesso.

## BLUE

Na etapa **BLUE**, todos os testes foram executados novamente e a cobertura foi analisada com o **JaCoCo**.

Resultado final:

```text
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Cobertura:

- **Instructions:** 100%
- **Branches:** 100%
- **Lines:** 100%
- **Methods:** 100%
- **Classes:** 100%

---

# API REST

O backend disponibiliza uma API REST para gerenciamento dos alunos.

Base:

```text
http://localhost:8080/alunos
```

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/alunos` | Cadastra um aluno |
| GET | `/alunos` | Lista todos os alunos |
| GET | `/alunos/{id}` | Busca um aluno pelo ID |
| DELETE | `/alunos/{id}` | Exclui um aluno |
| PUT | `/alunos/{id}/concluir-curso` | Registra a conclusão de um curso |
| PUT | `/alunos/{id}/liberar-cursos?media=7.5` | Libera 3 cursos adicionais **se a média for ≥ 7,0** (B1/ADR-002) |
| PUT | `/alunos/{id}/notificar` | Registra a notificação de mudança para PREMIUM |
| GET | `/ranking?limite=10` | Ranking por cursos concluídos (B1/ADR-001) |

Ao atingir **12 cursos concluídos**, o endpoint de conclusão de curso atualiza o plano para `PREMIUM` e registra o voucher.

---

# Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Especificação OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

O Swagger permite visualizar e testar os endpoints disponibilizados pela API.

---

# Banco de dados

A aplicação utiliza **PostgreSQL** como banco de dados principal quando executada através do Docker.

A tabela principal é:

```text
alunos
```

Ela armazena:

- ID;
- Plano;
- Cursos concluídos;
- Cursos adicionais permitidos;
- Possui voucher;
- Notificação de melhoria de plano.

## PostgreSQL via Docker

```text
Container: ac1-postgres
Database: ac1final
Username: postgres
Password: postgres
Porta: 5432
```

Conexão utilizada pelo backend dentro da rede Docker:

```text
jdbc:postgresql://postgres:5432/ac1final
```

---

# PGAdmin

O PGAdmin é executado em um container separado.

Acesso:

```text
http://localhost:5050
```

Credenciais:

```text
Email: admin@admin.com
Senha: admin
```

Para conectar ao PostgreSQL pelo PGAdmin:

```text
Host: postgres
Port: 5432
Database: ac1final
Username: postgres
Password: postgres
```

A tabela pode ser encontrada em:

```text
Servers
└── AC1 PostgreSQL
    └── Databases
        └── ac1final
            └── Schemas
                └── public
                    └── Tables
                        └── alunos
```

---

# Frontend

Foi desenvolvido um frontend utilizando **VueJS**.

O frontend permite:

- Listar alunos;
- Cadastrar alunos;
- Excluir alunos;
- Registrar conclusão de curso;
- Liberar 3 cursos adicionais;
- Registrar a notificação de mudança de plano;
- Visualizar plano;
- Visualizar cursos concluídos;
- Visualizar cursos adicionais;
- Visualizar voucher;
- Visualizar notificação.

Acesso:

```text
http://localhost:5173
```

O frontend é servido pelo Nginx quando executado com Docker.

---

# Docker

A aplicação foi preparada para execução utilizando **Docker Compose**, com quatro serviços:

```text
Frontend VueJS + Nginx  → localhost:5173
Backend Spring Boot     → localhost:8080
PostgreSQL              → localhost:5432
PGAdmin                 → localhost:5050
```

## Serviços

| Serviço | Container | Porta |
|---|---|---:|
| Frontend | `ac1-frontend` | 5173 |
| Backend | `ac1-backend` | 8080 |
| PostgreSQL | `ac1-postgres` | 5432 |
| PGAdmin | `ac1-pgadmin` | 5050 |

## Executando com Docker

É necessário ter o **Docker Desktop** instalado e em execução.

No diretório raiz:

```powershell
docker compose up --build
```

Para verificar os containers:

```powershell
docker compose ps
```

A execução validada apresentou:

```text
ac1-backend    Up
ac1-frontend   Up
ac1-pgadmin    Up
ac1-postgres   Up (healthy)
```

## Parar os containers

```powershell
docker compose down
```

Para remover também o volume do PostgreSQL:

```powershell
docker compose down -v
```

---

# Portas da aplicação

| Componente | URL |
|---|---|
| Frontend VueJS | http://localhost:5173 |
| API Spring Boot | http://localhost:8080/alunos |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI | http://localhost:8080/v3/api-docs |
| PGAdmin | http://localhost:5050 |
| PostgreSQL | localhost:5432 |

---

# Fluxo principal da User Story 2

```text
Aluno inicia no plano BASICO
          │
          ▼
Conclui cursos
          │
          ▼
Cursos concluídos >= 12?
       /       \
     NÃO       SIM
      │          │
      │          ▼
      │     Plano = PREMIUM
      │          │
      │          ▼
      │     Voucher = TRUE
      │
      ▼
Continua no fluxo normal
```

---

# Validação final

Foram realizadas validações dos principais componentes:

- Testes automatizados com JUnit 5;
- Cobertura de código com JaCoCo;
- API REST com Spring Boot;
- Swagger para documentação e testes dos endpoints;
- Persistência utilizando PostgreSQL;
- Administração do banco utilizando PGAdmin;
- Interface web utilizando VueJS;
- Execução integrada utilizando Docker Compose;
- Verificação dos containers com `docker compose ps`.
