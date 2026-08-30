package me.imshy.pictogram.testsupport;

import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for a module-integration test — the "bulk" of Pictogram's coverage (ADR-0007).
 * Slices the current module against the singleton {@link SharedPostgres}, truncating between
 * tests rather than rolling back (rollback doesn't reach async listeners).
 */
@ApplicationModuleTest
@ActiveProfiles("test")
public abstract class ModuleIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
    }

    @AfterEach
    void truncateAllTables() {
        new DatabaseCleaner(dataSource).truncateAll();
    }
}
