import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// App version: bumping it makes the release workflow publish a new GitHub release
val appVersionName = "0.4.0"

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
        // versionCode follows the version name, 0.4.0 becomes 400
        versionCode = appVersionName.split(".").let { (major, minor, patch) ->
            major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
        }
        versionName = appVersionName
        // Keys of the sites that have the posters, empty when they are not set and the cover of AniList is shown
        buildConfigField("String", "TMDB_TOKEN", "\"${keyOf("TMDB_TOKEN", "tmdb.token")}\"")
        buildConfigField("String", "TVDB_KEY", "\"${keyOf("TVDB_KEY", "tvdb.key")}\"")
        // Client ID of MyAnimeList, which gives the rating of the titles the countries do not, empty when it is not set
        buildConfigField("String", "MAL_CLIENT_ID", "\"${keyOf("MAL_CLIENT_ID", "mal.clientId")}\"")
        // Id of the API client registered on AniList for this app, empty until it is set the login says so
        buildConfigField("String", "ANILIST_CLIENT_ID", "\"${keyOf("ANILIST_CLIENT_ID", "anilist.clientId")}\"")
        // Id of a second API client of AniList whose redirect is the page that shows the token to copy, for the sign in with a token.
        // Empty until it is set, the sheet of the token then asks for one made by the user
        buildConfigField("String", "ANILIST_PIN_CLIENT_ID", "\"${keyOf("ANILIST_PIN_CLIENT_ID", "anilist.pinClientId")}\"")
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
    androidResources {
        // Supported app languages: English is the fallback
        localeFilters += listOf("en", "pt-rBR")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.youtube.player)
    testImplementation(libs.junit)
    // The JSON of Android is only a stub on the machine, the tests that read JSON need the real one
    testImplementation(libs.org.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
