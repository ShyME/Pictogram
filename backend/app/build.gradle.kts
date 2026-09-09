plugins {
    id("pictogram.java-library-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":identity"))
    implementation(project(":profile"))
    implementation(project(":media"))
    implementation(project(":post"))
    implementation(project(":social"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(libs.springdoc.openapi.starter.webmvc.api)
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-actuator")
    implementation("org.springframework.modulith:spring-modulith-observability")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Kafka for the one externalised flow (social -> notifications, ADR-0015). The
    // composition root is the only place these are wired: spring-modulith-events-jpa is the
    // outbox (the JPA event-publication registry), -jackson serialises the rows, and
    // -kafka relays them to the broker. social's three engagement events are @Externalized
    // onto pictogram.social (#196); nothing consumes the topic yet — ticket #197.
    implementation(libs.spring.kafka)
    implementation(libs.spring.modulith.events.kafka)
    implementation(libs.spring.modulith.events.jpa)
    implementation(libs.spring.modulith.events.jackson)

    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation(project(":test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.mock.oauth2.server)
}

base {
    archivesName = "pictogram"
}

springBoot {
    mainClass = "me.imshy.pictogram.PictogramApplication"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "pictogram.jar"
}

val openApiSpecFile = rootProject.layout.projectDirectory.file("openapi.json")

tasks.withType<Test>().configureEach {
    systemProperty("pictogram.openapi.file", openApiSpecFile.asFile.absolutePath)
}

tasks.register<Test>("generateOpenApiSpec") {
    description = "Boots the app and (re)writes backend/openapi.json from /v3/api-docs."
    group = "documentation"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    systemProperty("pictogram.openapi.generate", "true")
    filter { includeTestsMatching("me.imshy.pictogram.OpenApiContractTest") }
    outputs.upToDateWhen { false }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir.parentFile
    systemProperty("spring.profiles.active", "local")
}
