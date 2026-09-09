plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    implementation(project(":post"))

    // The @Externalized annotation on PostLiked / PostCommented / UserFollowed and the
    // EventExternalizationConfiguration that shapes them for Kafka (ADR-0015, #196). Just the
    // policy — the broker, the JPA outbox and the relay stay wired at the composition root.
    implementation(libs.spring.modulith.events.api)

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}
