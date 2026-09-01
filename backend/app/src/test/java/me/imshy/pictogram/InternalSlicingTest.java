package me.imshy.pictogram;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Within a module, the sub-packages of {@code internal} — the {@code web} edge, plus any
 * {@code internal.<cluster>} package a crowded module is carved into — must not depend on
 * one another. Composition happens in the module's {@code internal} root; a sub-package
 * reaching sideways into a sibling is bypassing that seam (a controller calling a mechanism
 * class directly instead of going through the module's façade). This is Spring Modulith's
 * {@code internal} rule applied one level down.
 *
 * <p>The pattern is structural — {@code (*)} matches any module and any sub-package — so a
 * new cluster package is covered automatically and needs no edit here. If this test fails
 * right after a package is split out, the split isn't clean: something in the new package
 * is coupled to a sibling and belongs in the {@code internal} root or in {@code shared-kernel}.
 */
class InternalSlicingTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("me.imshy.pictogram");

    @Test
    void internalSubpackagesDoNotDependOnEachOther() {
        slices()
                .matching("me.imshy.pictogram.(*).internal.(*)..")
                .should()
                .notDependOnEachOther()
                .check(CLASSES);
    }
}
