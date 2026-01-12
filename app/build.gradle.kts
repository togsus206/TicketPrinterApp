// app/build.gradle.kts
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android") 
}

android {
    namespace = "com.mval.ticketprinter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mval.ticketprinter"
        minSdk = 21
        targetSdk = 35
        versionCode = 15
        versionName = "11.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

   
    signingConfigs {
        create("release") {
            // Ruta al archivo keystore.properties en la raíz del proyecto
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()

            if (keystorePropertiesFile.exists()) {
                FileInputStream(keystorePropertiesFile).use {
                    keystoreProperties.load(it)
                }
            } else {
                println("ADVERTENCIA: keystore.properties no encontrado. Intentando usar variables de entorno.")
            }

            
            storeFile = rootProject.file("mval-valle-tpa.keystore") 

            // Obtiene las contraseñas y el alias del archivo de propiedades o variables de entorno
            storePassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("KEYSTORE_STORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: System.getenv("KEYSTORE_KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEYSTORE_KEY_PASSWORD")

            // Verificar que las propiedades no sean nulas después de intentar leerlas
            if (storePassword.isNullOrBlank() || keyAlias.isNullOrBlank() || keyPassword.isNullOrBlank()) {
                throw GradleException("ERROR: Faltan propiedades de firma (storePassword, keyAlias, keyPassword). Revisa keystore.properties o variables de entorno.")
            }
        }
    }

    
    buildTypes {
        release {
            isMinifyEnabled = true // Habilita ProGuard/R8 para optimización
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // Asigna la configuración de firma 'release'
        }
       
        debug {
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
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") 
    //------
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
}
