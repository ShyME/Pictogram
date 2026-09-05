package me.imshy.pictogram.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The one Postgres for the whole suite — a JVM-wide singleton it owns the
 * lifecycle of (started in the static block, stopped only at JVM exit).
 */
public final class SharedPostgres {

    private static final boolean REUSE = System.getenv("CI") == null;

    public static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:18-alpine").withReuse(REUSE);

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
