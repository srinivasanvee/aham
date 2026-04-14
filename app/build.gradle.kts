import java.util.Properties

// ---------------------------------------------------------------------------
// Keystore — local development
// ---------------------------------------------------------------------------
// Copy keystore.properties.template → keystore.properties (in project root,
// gitignored) and fill in your values for local release builds.
// In CI, signing credentials come from environment variables set in the
// GitHub Actions deploy workflow.

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sri.aham"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sri.aham"
        minSdk = 33
        targetSdk = 36

        // In CI, VERSION_CODE = GITHUB_RUN_NUMBER (auto-increments every deploy).
        // Locally defaults to 1 so the project always compiles without setup.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // litert-lm ships native libs; limit to ABIs that support it
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // ---------------------------------------------------------------------------
    // Signing
    // ---------------------------------------------------------------------------
    // Priority: keystore.properties (local) → environment variables (CI)
    signingConfigs {
        create("release") {
            val storePath = keystoreProperties.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_PATH")

            if (storePath != null) {
                storeFile     = file(storePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias      = keystoreProperties.getProperty("keyAlias")
                    ?: System.getenv("KEY_ALIAS") ?: ""
                keyPassword   = keystoreProperties.getProperty("keyPassword")
                    ?: System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.litertlm.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
