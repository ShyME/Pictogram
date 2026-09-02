dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // buildSrc is a separate build: the main build's auto-detected `gradle/libs.versions.toml`
    // is not visible here unless the same file is registered explicitly.
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "buildSrc"
