plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// App version: bumping it makes the release workflow publish a new GitHub release
val appVersionName = "0.0.1"

android {
    namespace = "com.kaisou.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kaisou.app"
        minSdk = 26
        targetSdk = 37
        // versionCode follows the version name, 0.0.1 becomes 1
        versionCode = appVersionName.split(".").let { (major, minor, patch) ->
            major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
        }
        versionName = appVersionName
    }

    // Release signing comes from environment variables set by the CI
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    // One APK per ABI plus a universal one, skipped when running from the IDE
    splits {
        abi {
            isEnable = !project.hasProperty("android.injected.invoked.from.ide")
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
            // Release key on the CI, debug key for local builds so the APK still installs
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
