import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.8")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}
