plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nikgapps.v2"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.nikgapps.v2"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "0.1.0-phase01.004"
    }
    signingConfigs {
        create("investigation") {
            storeFile = file(System.getenv("V2_KEYSTORE_PATH") ?: "../private.keystore")
            storePassword = System.getenv("V2_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("V2_KEY_ALIAS")
            keyPassword = System.getenv("V2_KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("investigation")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.navigation:navigation-compose:2.10.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
