plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cr.kind.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cr.kind.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            // La interfaz (HTML/CSS/JS), las frases y las imágenes viven en
            // ../../www y se empaquetan tal cual dentro del APK. Así hay una
            // sola copia: la misma que se usa como PWA en el navegador.
            assets.srcDirs("src/main/assets", rootProject.file("../www"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
