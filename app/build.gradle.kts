import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

// debug.keystore is committed to the repo root with standard Android debug credentials.
// Referencing it directly here (instead of relying on CI to copy it to ~/.android/)
// guarantees every build — local or CI — uses the exact same certificate,
// so Obtainium can install updates without requiring an uninstall first.
val committedKeystore = rootProject.file("debug.keystore")

android {
    namespace = "com.ncportal.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ncportal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Stable signing: use the committed keystore when it exists (local dev + CI).
        if (committedKeystore.exists()) {
            create("stable") {
                storeFile     = committedKeystore
                storePassword = "android"
                keyAlias      = "androiddebugkey"
                keyPassword   = "android"
            }
        }
        // Optional production keystore via keystore.properties (gitignored).
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile     = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias      = keystoreProperties.getProperty("keyAlias")
                keyPassword   = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Override the auto-generated per-machine debug key with our stable committed key.
            signingConfig = signingConfigs.findByName("stable")
                ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.findByName("stable")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.ui.tooling)
}
