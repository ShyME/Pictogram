package me.imshy.pictogram.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The one PostgreSQL container for the whole build — Testcontainers' "singleton container"
 * pattern: started once, never stopped, reaped at JVM shutdown. Set
 * {@code testcontainers.reuse.enable=true} in {@code ~/.testcontainers.properties} to keep
 * it alive across local runs too. Reuse is off on CI (fresh runner every time, so a leaked
 * container would only waste resources).
 */
public final class SharedPostgres {

    private static final boolean REUSE = System.getenv("CI") == null;

    public static final PostgreSQLContainer INSTANCE =
            new PostgreSQLContainer("postgres:17-alpine").withReuse(REUSE);

    static {
        INSTANCE.start();
    }

    private SharedPostgres() {
    }

    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }
}
