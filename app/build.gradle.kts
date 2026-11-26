plugins { id("com.android.application") }
android {
    namespace = "com.nikgapps.v2"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.nikgapps.v2"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-phase00.001"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
