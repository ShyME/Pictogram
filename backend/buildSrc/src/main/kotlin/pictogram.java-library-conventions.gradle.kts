import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
    id("io.spring.dependency-management")
    id("pictogram.code-quality-conventions")
}

// Precompiled script plugins don't get generated version-catalog accessors; reach the
// catalog through its extension instead. https://github.com/gradle/gradle/issues/15383
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

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

// spring-modulith ships no BOM_COORDINATES constant, so its coordinate is spelled out.
val modulithBom =
    "org.springframework.modulith:spring-modulith-bom:${
        libs.findVersion("spring-modulith").get().requiredVersion
    }"

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
        mavenBom(modulithBom)
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
