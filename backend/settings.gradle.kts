plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "pictogram"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

listOf(
    "shared-kernel",
    "test-support",
    "identity",
    "profile",
    "media",
    "post",
    "follow",
    "feed",
    "engagement",
    "app",
).forEach { include(it) }
