plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.rtc"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // LiveKit Cloud "development token server" id. Kept out of the repo: set
        // livekit.tokenServerId in local.properties or LIVEKIT_TOKEN_SERVER_ID in the environment.
        val tokenServerId = providers.environmentVariable("LIVEKIT_TOKEN_SERVER_ID")
            .orElse(
                providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText
                    .map { text ->
                        text.lineSequence()
                            .map { it.trim() }
                            .firstOrNull { it.startsWith("livekit.tokenServerId=") }
                            ?.substringAfter("=")
                            ?.trim()
                            .orEmpty()
                    },
            )
            .orElse("")
        buildConfigField("String", "TOKEN_SERVER_ID", "\"${tokenServerId.get()}\"")
    }
}

dependencies {
    implementation(libs.livekit.android)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
