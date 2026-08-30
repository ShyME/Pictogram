// Not a bounded context: the shared module-integration harness, consumed as
// testImplementation(project(":test-support")). Deps are `api` so they reach consumers' tests.

plugins {
    id("pictogram.java-library-conventions")
}

dependencies {
    api(project(":shared-kernel"))

    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.modulith:spring-modulith-starter-test")
    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:testcontainers-junit-jupiter")
    api("org.testcontainers:testcontainers-postgresql")
    api("org.postgresql:postgresql")
}
