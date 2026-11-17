plugins {
    id("motointercom.android.feature")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.feature.group"
}

dependencies {
    implementation(project(":core:service"))

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
