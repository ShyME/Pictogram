plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.1"
}

group = "me.imshy"

version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // The reactive stack, not spring-boot-starter-web — see ADR-0014.
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    // Builds and runs the actual Dockerfile in ChatDockerImageTest — version comes from
    // Boot's BOM (testcontainers-bom), same as backend.
    testImplementation("org.testcontainers:testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.97.0")
        removeUnusedImports()
        importOrder()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}

base {
    archivesName = "chat"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "chat.jar"
}

// ChatDockerImageTest builds the real image and runs it as a container — slow, and it
// asserts against what `docker build` produces rather than test classes, so (mirroring
// backend's blackbox convention) it's opt-in via -PincludeBlackbox rather than part of the
// default `check`.
val includeBlackbox = project.hasProperty("includeBlackbox")

tasks.named<Test>("test") {
    useJUnitPlatform {
        if (includeBlackbox) includeTags("blackbox") else excludeTags("blackbox")
    }
    filter { isFailOnNoMatchingTests = !includeBlackbox }
    if (includeBlackbox) doNotTrackState("asserts against a freshly built image")
}
