import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

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

    dependencies {
        "implementation"(libs.lib("kotlinx-coroutines-android"))

        "testImplementation"(platform(libs.lib("junit-bom")))
        "testImplementation"(libs.lib("junit-jupiter"))
        "testRuntimeOnly"(libs.lib("junit-platform-launcher"))
        "testImplementation"(libs.lib("mockk"))
        "testImplementation"(libs.lib("turbine"))
        "testImplementation"(libs.lib("kotlinx-coroutines-test"))
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
        "implementation"(libs.lib("compose-material-icons-core"))
        "debugImplementation"(libs.lib("compose-ui-tooling"))
    }
}
