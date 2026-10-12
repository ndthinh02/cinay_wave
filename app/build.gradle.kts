plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
fun backendUrl(name: String, fallback: String): String {
    val value = providers.gradleProperty(name).orElse(fallback).get()
    require(value.none { it.isWhitespace() }) { "$name must not contain whitespace" }
    require(value.endsWith("/")) { "$name must end with /" }
    require(value.startsWith("https://") || (name == "DEBUG_API_URL" && value.startsWith("http://")))
    return "\"${value.replace("\"", "")}\""
}
val googleAuthEnabled = providers.gradleProperty("ENABLE_GOOGLE_AUTH").orElse("true").get().toBoolean()
android {
    namespace = "com.cinaywave.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cinaywave.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").orElse("").get().replace("\"", "")}\"")
    }
    buildTypes {
        debug { buildConfigField("String", "API_URL", backendUrl("DEBUG_API_URL", "http://192.168.2.197:3000/api/")) }
        release {
            buildConfigField(
                "String",
                "API_URL",
                backendUrl("RELEASE_API_URL", "https://configure-backend.invalid/api/")
            )
            isMinifyEnabled = false
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("main").java.srcDir(if (googleAuthEnabled) "src/googleAuth/kotlin" else "src/emailOnly/kotlin")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    if (googleAuthEnabled) {
        implementation("androidx.credentials:credentials:1.5.0")
        implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
        // 1.2.1 uses Kotlin 2.4 metadata; keep the current Kotlin 2.1 toolchain.
        implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    }
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okio:okio:3.9.1")
}
// Keep the Lifecycle family aligned; Compose otherwise promotes only some artifacts.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.lifecycle") useVersion("2.8.3")
    }
}
