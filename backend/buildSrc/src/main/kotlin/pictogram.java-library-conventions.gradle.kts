import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
    id("io.spring.dependency-management")
    id("pictogram.code-quality-conventions")
}

group = "me.imshy"

version = "0.1.0-SNAPSHOT"

base {
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

val includeBlackbox = project.hasProperty("includeBlackbox")

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (includeBlackbox) includeTags("blackbox") else excludeTags("blackbox")
    }
    filter { isFailOnNoMatchingTests = !includeBlackbox }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
