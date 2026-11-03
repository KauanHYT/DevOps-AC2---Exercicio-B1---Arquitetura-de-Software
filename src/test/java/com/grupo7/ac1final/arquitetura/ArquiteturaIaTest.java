package com.grupo7.ac1final.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Desafio 3 / ADR-003: a fronteira com o serviço Python fica restrita ao adapter HTTP.
 * A aplicação só conhece portas; trocar HTTP por fila ou por outro provedor de IA
 * não toca nos casos de uso.
 */
class ArquiteturaIaTest {

    private static final String IA = "com.grupo7.ac1final.ia";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.grupo7.ac1final");

    @Test
    void dominioDaIaEJavaPuro() {
        ArchRule regra = noClasses().that().resideInAPackage(IA + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", IA + ".application..", IA + ".adapter..");
        regra.check(CLASSES);
    }

    @Test
    void aplicacaoNaoConheceHttpNemAdapters() {
        ArchRule regra = noClasses().that().resideInAPackage(IA + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        IA + ".adapter..", "org.springframework.web..", "org.springframework.http..")
                .because("a falha da IA é tratada pela porta (IaIndisponivelException), não por exceções HTTP");
        regra.check(CLASSES);
    }

    @Test
    void controllersNaoChamamOClienteHttpDiretamente() {
        ArchRule regra = noClasses().that().resideInAPackage(IA + ".adapter.web..")
                .should().dependOnClassesThat().resideInAPackage(IA + ".adapter.http..")
                .because("o fallback mora no caso de uso; o controller não pode pular essa regra");
        regra.check(CLASSES);
    }
}