package me.imshy.pictogram.testsupport;

import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared setup for a module-integration test — the "bulk" of Pictogram's coverage (ADR-0007):
 * the singleton {@link SharedPostgres}, the {@code test} profile, and table truncation
 * between tests (rollback doesn't reach async listeners).
 *
 * <p>This class deliberately does <strong>not</strong> carry {@code @ApplicationModuleTest}:
 * Spring Modulith resolves the module under test from the class that <em>declares</em> that
 * annotation, so it must sit on a base class (or the test itself) in the module's own
 * package, not here in {@code test-support}.
 */
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
