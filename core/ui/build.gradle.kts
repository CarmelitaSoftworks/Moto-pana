plugins {
    id("motointercom.android.library")
    id("motointercom.android.compose")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.ui"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
