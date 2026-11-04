package com.grupo7.ac1final.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Desafio 4 / ADR-004: o caso de uso conhece só a porta NotificadorPromocao.
 * Broker, outbox e HTTP ficam nos adapters, e trocar RabbitMQ por Kafka afeta um pacote só.
 */
class ArquiteturaEventosTest {

    private static final String ALUNO = "com.grupo7.ac1final.aluno";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.grupo7.ac1final");

    @Test
    void aplicacaoNaoConheceBrokerNemHttp() {
        ArchRule regra = noClasses().that().resideInAPackage(ALUNO + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.amqp..", "org.springframework.web..", "org.springframework.http..");
        regra.check(CLASSES);
    }

    @Test
    void somenteOAdapterDeMensageriaConheceORabbitMQ() {
        ArchRule regra = noClasses().that().resideOutsideOfPackage(ALUNO + ".adapter.mensageria..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.amqp..")
                .because("trocar o broker deve afetar apenas aluno.adapter.mensageria");
        regra.check(CLASSES);
    }

    @Test
    void eventoDeDominioNaoDependeDeInfraestrutura() {
        ArchRule regra = noClasses().that().resideInAPackage(ALUNO + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..");
        regra.check(CLASSES);
    }
}