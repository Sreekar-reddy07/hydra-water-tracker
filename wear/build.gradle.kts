plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.sreekar.hydra.wear"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.sreekar.hydra.wear"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.7.0" }
}
dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
}
