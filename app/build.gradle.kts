plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.paytimeshift.pts"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.paytimeshift.pts"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.1.7"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("preview-signing/pts-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            providers.environmentVariable("PTS_UPLOAD_STORE_FILE").orNull?.let { storeFile = file(it) }
            storePassword = providers.environmentVariable("PTS_UPLOAD_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("PTS_UPLOAD_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("PTS_UPLOAD_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
