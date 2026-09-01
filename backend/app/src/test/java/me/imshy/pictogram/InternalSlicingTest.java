package me.imshy.pictogram;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

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
