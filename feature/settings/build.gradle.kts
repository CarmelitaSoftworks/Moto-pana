plugins {
    id("motointercom.android.feature")
    id("motointercom.hilt")
}

android {
    namespace = "com.carmelitasoftworks.motointercom.feature.settings"
}

dependencies {
    implementation(project(":core:audio"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
