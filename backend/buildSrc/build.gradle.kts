import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.dependency.management.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.modernizer.plugin)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}
