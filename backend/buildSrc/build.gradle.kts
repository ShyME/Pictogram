import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.8")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}

// Pin the target so Kotlin doesn't try (and fail) to target the JDK running Gradle when
// that JDK is newer than Kotlin supports.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}
