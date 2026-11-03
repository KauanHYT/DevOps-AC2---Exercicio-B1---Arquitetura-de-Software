package com.grupo7.ac1final.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Desafio 1 / ADR-001 — Monólito Modular.
 * Estas regras transformam a decisão arquitetural em teste: se alguém criar
 * um atalho entre módulos, o build quebra.
 */
class ArquiteturaModularTest {

    private static final String BASE = "com.grupo7.ac1final";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void rankingSoConheceAApiPublicaDoAluno() {
        ArchRule regra = noClasses().that().resideInAPackage(BASE + ".ranking..")
                .should().dependOnClassesThat(
                        resideInAPackage(BASE + ".aluno..").and(resideOutsideOfPackage(BASE + ".aluno.api..")))
                .because("o módulo ranking deve usar apenas aluno.api (ADR-001)");
        regra.check(CLASSES);
    }

    @Test
    void iaSoConheceAApiPublicaDoAluno() {
        ArchRule regra = noClasses().that().resideInAPackage(BASE + ".ia..")
                .should().dependOnClassesThat(
                        resideInAPackage(BASE + ".aluno..").and(resideOutsideOfPackage(BASE + ".aluno.api..")))
                .because("o módulo ia recebe o progresso do aluno só por aluno.api (ADR-001, ADR-003)");
        regra.check(CLASSES);
    }

    @Test
    void alunoNaoDependeDoRanking() {
        ArchRule regra = noClasses().that().resideInAPackage(BASE + ".aluno..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".ranking..")
                .because("a dependência entre módulos é unidirecional: ranking -> aluno.api");
        regra.check(CLASSES);
    }

    @Test
    void modulosNaoPossuemCiclos() {
        ArchRule regra = slices().matching(BASE + ".(*)..").should().beFreeOfCycles();
        regra.check(CLASSES);
    }
}
