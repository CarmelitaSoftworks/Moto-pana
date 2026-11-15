plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.service"
}

dependencies {
    implementation(project(":core:audio"))
    implementation(project(":core:rtc"))
    implementation(libs.androidx.core.ktx)
}
