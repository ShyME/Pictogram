// A separate Gradle build (ADR-0014) can't share backend/'s version catalog without
// reintroducing the coupling that ADR is deliberately practising splitting away from — so
// these versions are copied from backend/gradle/libs.versions.toml by hand, not pinned
// there. Dependabot scans this build independently too (.github/dependabot.yml); when its
// bump PR lands, or when backend's does, check whether the other should follow.
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

// Mirrors backend's pictogram.code-quality-conventions.gradle.kts ruleset by hand — same
// reason as the plugin versions above. Keep the two in sync when one changes.
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
