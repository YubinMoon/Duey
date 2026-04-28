// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.5.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_function-naming" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.5.0")
    }
}
