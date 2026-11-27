plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nikgapps.v2"
    testNamespace = "com.nikgapps.v2.test"
    compileSdk = 37
    testBuildType = "release"
    defaultConfig {
        applicationId = "com.nikgapps.v2"
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "com.nikgapps.v2.FixtureInstrumentation"
        versionCode = 24
        versionName = "0.1.0-phase04.004"
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

// The pinned signing workflow already runs lintRelease; include the storage checks in that verification.
tasks.matching { it.name == "lintRelease" }.configureEach {
    dependsOn("testReleaseUnitTest")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0") // Same catalog parser dependency as the original app.
    compileOnly("androidx.startup:startup-runtime:1.2.0") // Already present at runtime; explicit provider merge is checked by lint.
    implementation("com.squareup.okhttp3:okhttp:5.5.0") // Same version as the original app.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // JVM tests only; Android supplies org.json in production.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.navigation:navigation-compose:2.10.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
