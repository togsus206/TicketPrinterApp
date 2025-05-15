// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android") version "1.9.0" // Asegúrate de que la versión coincida con la del raíz
}

android {
    namespace = "com.mval.ticketprinter"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.mval.ticketprinter"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
