plugins {
    java
    id("com.diffplug.spotless")
    id("io.github.andygoossens.modernizer")
}

// Precompiled script plugins don't get generated version-catalog accessors; reach the
// catalog through its extension instead. https://github.com/gradle/gradle/issues/15383
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat(libs.findVersion("palantir-java-format").get().requiredVersion)
        removeUnusedImports()
        importOrder()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

modernizer {
    failOnViolations = true
    includeTestClasses = true
    javaVersion = "25"
}
