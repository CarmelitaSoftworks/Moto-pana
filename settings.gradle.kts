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
        // LiveKit pulls a few transitive artifacts from JitPack.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
    }
}

rootProject.name = "moto-intercom"

include(":app")
include(":core:audio")
include(":core:rtc")
include(":core:service")
include(":feature:group")
include(":feature:ride")
include(":feature:settings")
