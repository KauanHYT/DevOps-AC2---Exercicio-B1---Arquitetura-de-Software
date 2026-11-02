package com.grupo7.ac1final.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Desafio 2 / ADR-002 — Ports &amp; Adapters no módulo aluno.
 * A dependência aponta sempre para dentro: adapters -&gt; aplicação -&gt; domínio.
 */
class ArquiteturaHexagonalTest {

    private static final String ALUNO = "com.grupo7.ac1final.aluno";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.grupo7.ac1final");

    @Test
    void dominioNaoDependeDeFrameworkNemDeOutrasCamadas() {
        ArchRule regra = noClasses().that().resideInAPackage(ALUNO + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..",
                        ALUNO + ".application..", ALUNO + ".adapter..")
                .because("as regras de gamificação devem ser Java puro (ADR-002)");
        regra.check(CLASSES);
    }

    @Test
    void aplicacaoNaoDependeDeAdaptersNemDeJpa() {
        ArchRule regra = noClasses().that().resideInAPackage(ALUNO + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ALUNO + ".adapter..", "jakarta.persistence..", "org.springframework.data..")
                .because("a aplicação conversa com a persistência só pela porta AlunoRepositoryPort");
        regra.check(CLASSES);
    }

    @Test
    void adapterWebNaoAcessaPersistenciaDiretamente() {
        ArchRule regra = noClasses().that().resideInAPackage(ALUNO + ".adapter.web..")
                .should().dependOnClassesThat().resideInAPackage(ALUNO + ".adapter.persistence..")
                .because("na AC1 o controller manipulava a entidade JPA e duplicava regras");
        regra.check(CLASSES);
    }

    @Test
    void somenteOAdapterDePersistenciaUsaJpa() {
        ArchRule regra = noClasses().that().resideOutsideOfPackage(ALUNO + ".adapter.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
                .because("trocar o banco ou o ORM deve afetar apenas um pacote");
        regra.check(CLASSES);
    }
}
