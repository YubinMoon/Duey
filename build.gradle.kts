// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
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
        target("*.gradle.kts", "app/*.gradle.kts", "server/*.gradle.kts")
        ktlint("1.5.0")
    }
    java {
        target("server/src/**/*.java")
        googleJavaFormat("1.24.0").aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}

tasks.register("format") {
    group = "formatting"
    description = "Formats app and server source code."
    dependsOn("spotlessApply")
}

project(":app") {
    tasks.register("format") {
        group = "formatting"
        description = "Formats Android app source code."
        dependsOn(rootProject.tasks.named("spotlessKotlinApply"))
        dependsOn(rootProject.tasks.named("spotlessKotlinGradleApply"))
    }
}

project(":server") {
    tasks.register("format") {
        group = "formatting"
        description = "Formats server source code."
        dependsOn(rootProject.tasks.named("spotlessJavaApply"))
        dependsOn(rootProject.tasks.named("spotlessKotlinGradleApply"))
    }
}
