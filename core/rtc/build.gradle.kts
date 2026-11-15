plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.rtc"
}

dependencies {
    implementation(libs.livekit.android)
}
