plugins {
    base
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        target(
            "*.gradle.kts",
            "*/*.gradle.kts",
            "buildSrc/*.gradle.kts",
            "buildSrc/src/main/kotlin/*.gradle.kts",
        )
        // kotlinlangStyle keeps the 4-space indent .editorconfig sets; plain ktfmt() is 2-space.
        ktfmt().kotlinlangStyle()
    }
}
