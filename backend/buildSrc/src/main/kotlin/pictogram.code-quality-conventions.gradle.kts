plugins {
    id("com.diffplug.spotless")
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
