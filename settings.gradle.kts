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
    }
}

rootProject.name = "moto-intercom"

include(":app")
include(":core:audio")
include(":core:rtc")
include(":core:service")
include(":core:ui")
include(":feature:group")
include(":feature:ride")
include(":feature:settings")
