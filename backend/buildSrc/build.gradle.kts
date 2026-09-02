import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.1")
    implementation(
        "io.github.andygoossens.modernizer:io.github.andygoossens.modernizer.gradle.plugin:2.0.0"
    )
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}
