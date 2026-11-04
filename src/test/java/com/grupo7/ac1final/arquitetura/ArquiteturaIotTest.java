package com.grupo7.ac1final.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Desafio 5 / ADR-005: o Core recebe eventos IoT sem se acoplar a dispositivos.
 * Quem fala MQTT é o iot-gateway; o Core só conhece o evento já traduzido.
 */
class ArquiteturaIotTest {

    private static final String IOT = "com.grupo7.ac1final.iot";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.grupo7.ac1final");

    @Test
    void nenhumaClasseDoCoreConheceMqtt() {
        ArchRule regra = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.eclipse.paho..", "org.springframework.integration.mqtt..", "com.hivemq..")
                .because("o protocolo dos dispositivos fica no iot-gateway, fora do Core (ADR-005)");
        regra.check(CLASSES);
    }

    @Test
    void dominioIotEJavaPuro() {
        ArchRule regra = noClasses().that().resideInAPackage(IOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", IOT + ".application..", IOT + ".adapter..");
        regra.check(CLASSES);
    }

    @Test
    void aplicacaoIotNaoConheceAdaptersNemJpaNemWeb() {
        ArchRule regra = noClasses().that().resideInAPackage(IOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        IOT + ".adapter..", "jakarta.persistence..", "org.springframework.data..",
                        "org.springframework.web..", "org.springframework.http..");
        regra.check(CLASSES);
    }
}
