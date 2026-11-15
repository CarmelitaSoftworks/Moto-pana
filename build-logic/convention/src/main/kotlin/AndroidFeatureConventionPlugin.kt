import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A UI feature module: library + Compose + Hilt + lifecycle/navigation helpers. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("motointercom.android.library")
        pluginManager.apply("motointercom.android.compose")
        pluginManager.apply("motointercom.hilt")

        dependencies {
            "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))
            "implementation"(libs.lib("androidx-lifecycle-viewmodel-compose"))
            "implementation"(libs.lib("androidx-hilt-navigation-compose"))
        }
    }
}
