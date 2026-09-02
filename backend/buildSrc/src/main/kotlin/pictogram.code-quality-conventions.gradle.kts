plugins {
    java
    id("com.diffplug.spotless")
    id("io.github.andygoossens.modernizer")
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.97.0")
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
