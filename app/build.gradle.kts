import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// App version: bumping it makes the release workflow publish a new GitHub release
val appVersionName = "0.0.1"

// Keys of the services come from the environment of the CI or from the ignored local.properties, never from the code
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun keyOf(env: String, property: String): String = System.getenv(env) ?: localProperties.getProperty(property).orEmpty()

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
        // Empty when the key is not set, the artwork is then simply not shown
        buildConfigField("String", "TMDB_TOKEN", "\"${keyOf("TMDB_TOKEN", "tmdb.token")}\"")
        buildConfigField("String", "TVDB_KEY", "\"${keyOf("TVDB_KEY", "tvdb.key")}\"")
        // Id of the API client registered on AniList for this app, empty until it is set the login says so
        buildConfigField("String", "ANILIST_CLIENT_ID", "\"${keyOf("ANILIST_CLIENT_ID", "anilist.clientId")}\"")
        buildConfigField("String", "MAL_CLIENT_ID", "\"${keyOf("MAL_CLIENT_ID", "mal.clientId")}\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
