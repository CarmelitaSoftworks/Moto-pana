plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.rtc"
}

dependencies {
    // The LiveKit SDK is added in work order step 3, together with the code that uses it.
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
