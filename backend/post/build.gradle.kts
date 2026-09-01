plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    // post owns the `post` schema (ADR-0009, module ordinal 4). The starter brings only
    // flyway-core; Flyway needs the database module on the classpath to recognise Postgres.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // A Post holds a MediaId and, on publish, checks that MediaId resolves to media the
    // author owns — via media's published MediaCatalog interface (CONTEXT-MAP: post → media).
    implementation(project(":media"))
}
