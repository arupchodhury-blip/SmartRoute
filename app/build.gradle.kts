plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ca.smartroute.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ca.smartroute.app"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
