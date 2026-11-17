plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.nearby"
}

dependencies {
    api(project(":core:rtc"))
    implementation(project(":core:audio"))

    // Offline group mode: phone-to-phone over Bluetooth / Wi-Fi without internet (CLAUDE.md §4, fase 3).
    implementation(libs.play.services.nearby)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
