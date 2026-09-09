plugins {
    id("pictogram.java-library-conventions")
}

// Precompiled script plugins don't get generated version-catalog accessors.
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":shared-kernel"))

    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.modulith:spring-modulith-starter-test")
    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:testcontainers-junit-jupiter")
    api("org.testcontainers:testcontainers-postgresql")
    api("org.postgresql:postgresql")

    // The one Kafka container for the whole suite (ADR-0015): the app's relay test (#196)
    // and the notifications consumer suite (#197) point at it, nothing else. spring-boot-
    // starter-kafka rides along so the consumer factory can be built there — which also puts
    // KafkaAutoConfiguration on every module slice's test classpath, so the shared test base
    // excludes it (see application-test.yml).
    api("org.springframework.boot:spring-boot-starter-kafka")
    api(libs.findLibrary("testcontainers-kafka").get())
}
