import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Base conventions for every subproject: JDK 25 toolchain, the Spring Boot + Modulith BOMs, JUnit 5.

plugins {
    `java-library`
    id("io.spring.dependency-management")
}

group = "me.imshy"
version = "0.1.0-SNAPSHOT"

base {
    // :app overrides this to plain "pictogram" — the deployable artifact.
    archivesName = "pictogram-${project.name}"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.0.8")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
