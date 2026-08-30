plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    // Backend-driven OIDC Authorization Code + PKCE against Google (ADR-0004). The client
    // secret stays here; the browser only follows redirects.
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // identity owns the first Flyway-managed schema (ADR-0009). The starter brings only
    // flyway-core; Flyway 11 needs the database module on the classpath to recognise Postgres.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // The OIDC success handler and the auth endpoints touch the servlet API; the container
    // itself is provided by :app at runtime (as in shared-kernel).
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework.security:spring-security-test")
}
