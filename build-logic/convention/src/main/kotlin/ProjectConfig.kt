import com.android.build.api.dsl.CommonExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

/** Shared Android + Kotlin settings for every module (app and libraries). */
internal fun Project.configureKotlinAndroid(android: CommonExtension<*, *, *, *, *, *>) {
    android.apply {
        compileSdk = libs.version("compileSdk").toInt()

        defaultConfig {
            minSdk = libs.version("minSdk").toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions {
            unitTests.all { it.useJUnitPlatform() }
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }

    // Only the JUnit 5 baseline; modules add MockK / Turbine / coroutines-test as they need them.
    dependencies {
        "testImplementation"(platform(libs.lib("junit-bom")))
        "testImplementation"(libs.lib("junit-jupiter"))
        "testRuntimeOnly"(libs.lib("junit-platform-launcher"))
    }

    configureLint()
}

/** ktlint + detekt with the shared config (CLAUDE.md §7). */
internal fun Project.configureLint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        android.set(true)
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    }
}

/** Enables Jetpack Compose and adds the common Compose dependencies. */
internal fun Project.configureCompose(android: CommonExtension<*, *, *, *, *, *>) {
    android.buildFeatures.compose = true

    dependencies {
        "implementation"(platform(libs.lib("compose-bom")))
        "implementation"(libs.lib("compose-ui"))
        "implementation"(libs.lib("compose-ui-tooling-preview"))
        "implementation"(libs.lib("compose-material3"))
        "debugImplementation"(libs.lib("compose-ui-tooling"))
    }
}
