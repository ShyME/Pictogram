package me.imshy.pictogram.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The one Postgres for the whole suite — a JVM-wide singleton it owns the lifecycle of (started in
 * the static block, stopped only at JVM exit). Contexts get its coordinates through {@link
 * #registerTo}: app tests wrap it in a {@code DynamicPropertyRegistrar} bean, {@code
 * @ApplicationModuleTest} slices in the one shared {@code @DynamicPropertySource} on {@link
 * ModuleIntegrationTest}. Not a {@code @ServiceConnection} bean — that would hand the singleton to
 * Spring's Testcontainers lifecycle, and one failed context refresh would stop it for all the
 * others. A single {@code registerTo} call, not the per-class drift #78 removed.
 */
public final class SharedPostgres {

    private static final boolean REUSE = System.getenv("CI") == null;

    public static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:17-alpine").withReuse(REUSE);

    static {
        INSTANCE.start();
    }

    private SharedPostgres() {}

    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }
}
