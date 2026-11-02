// Build script modul app.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rizky.screentranslator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rizky.screentranslator"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    // AndroidX inti
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // ML Kit: OCR teks (Latin, on-device, sudah termasuk modelnya)
    implementation("com.google.mlkit:text-recognition:2.0.0")
    // ML Kit: terjemahan EN -> ID (model diunduh saat runtime)
    implementation("com.google.mlkit:translate:17.0.3")
}
