plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// `./gradlew ktlintCheck detekt` also lints the convention plugins in build-logic.
listOf("ktlintCheck", "ktlintFormat", "detekt").forEach { name ->
    tasks.register(name) {
        group = "verification"
        dependsOn(gradle.includedBuild("build-logic").task(":convention:$name"))
    }
}
