import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A UI feature module: library + Compose + the shared UI kit.
 * Hilt is opt-in (`motointercom.hilt`) once a feature gets its first ViewModel.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("motointercom.android.library")
        pluginManager.apply("motointercom.android.compose")

        dependencies {
            "implementation"(project(":core:ui"))
        }
    }
}
