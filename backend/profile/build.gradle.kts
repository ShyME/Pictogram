plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    // profile owns the `profile` schema (ADR-0009). The starter brings only flyway-core;
    // Flyway needs the database module on the classpath to recognise Postgres.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}
