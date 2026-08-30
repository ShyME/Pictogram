package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** The boundary guardrail: fails the build on an illegal cross-module dependency or a cycle. */
class ModulithStructureTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(
            PictogramApplication.class,
            JavaClass.Predicates.resideInAPackage("me.imshy.pictogram.testsupport.."));

    @Test
    void everyBoundedContextIsAModule() {
        var names = MODULES.stream()
                .map(module -> module.getIdentifier().toString())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder(
                "identity", "profile", "media", "post", "follow", "feed", "engagement", "shared");
    }

    @Test
    void modulesRespectTheirBoundaries() {
        MODULES.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();

        assertThat(Path.of("build", "spring-modulith-docs", "components.puml")).exists();
    }
}
