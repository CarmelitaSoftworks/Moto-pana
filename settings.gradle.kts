pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LiveKit depends on audioswitch, published only on JitPack.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.davidliu") }
        }
    }
}

rootProject.name = "moto-intercom"

include(":app")
include(":core:audio")
include(":core:rtc")
include(":core:nearby")
include(":core:service")
include(":core:ui")
include(":feature:group")
include(":feature:ride")
include(":feature:settings")
