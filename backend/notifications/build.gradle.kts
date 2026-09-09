plugins {
    id("pictogram.spring-module-conventions")
}

dependencies {
    // PostDeleted — consumed in-process (synchronous @EventListener, ADR-0015 / #138) to
    // purge a deleted post's notifications.
    implementation(project(":post"))

    // The hand-written spring-kafka @KafkaListener on pictogram.social (ADR-0015). Unlike the
    // producer side (wired at the composition root), the consumer is this module's own code,
    // so the Kafka client and listener infrastructure belong here.
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Test-only: the undo events (PostUnliked etc.) this module deliberately does NOT consume
    // are asserted against social's real event types.
    testImplementation(project(":social"))
}
