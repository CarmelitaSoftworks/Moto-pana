plugins {
    id("motointercom.android.application")
    id("motointercom.hilt")
}

val appVersionCode = 4
val appVersionName = "0.3.1"

// APKs come out as MotoIntercom-<version>-<buildType>.apk, e.g. MotoIntercom-0.3.0-debug.apk.
base {
    archivesName.set("MotoIntercom-$appVersionName")
}

android {
    namespace = "com.carmelitasoftworks.motointercom"

    defaultConfig {
        applicationId = "com.carmelitasoftworks.motointercom"
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            // Real phones only; skipping the emulator ABIs halves the APK (WebRTC is ~12 MB per ABI).
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:audio"))
    implementation(project(":core:rtc"))
    implementation(project(":core:service"))
    implementation(project(":core:ui"))
    implementation(project(":feature:group"))
    implementation(project(":feature:ride"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons.core)
}
