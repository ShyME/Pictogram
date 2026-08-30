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
    implementation(project(":follow"))
    implementation(project(":feed"))
    implementation(project(":engagement"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-actuator")
    implementation("org.springframework.modulith:spring-modulith-observability")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation(project(":test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
}

base {
    archivesName = "pictogram"
}

springBoot {
    mainClass = "me.imshy.pictogram.PictogramApplication"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // Stable name so the Dockerfile's COPY doesn't depend on the version string.
    archiveFileName = "pictogram.jar"
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // Run from the repo root so compose.dev.yaml (one level up from the Gradle build) resolves.
    workingDir = rootProject.projectDir.parentFile
    systemProperty("spring.profiles.active", "local")
}
