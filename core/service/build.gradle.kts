plugins {
    id("motointercom.android.library")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.core.service"
}

dependencies {
    api(project(":core:audio"))
    api(project(":core:rtc"))
    api(project(":core:nearby"))

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
