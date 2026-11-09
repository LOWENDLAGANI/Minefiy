plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.minetallest.minefiy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minetallest.minefiy"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val b2AccessKeyId = localProperties.getProperty("b2.access.key.id") ?: ""
        val b2SecretAccessKey = localProperties.getProperty("b2.secret.access.key") ?: ""
        val b2BucketName = localProperties.getProperty("b2.bucket.name") ?: ""
        val b2Endpoint = localProperties.getProperty("b2.endpoint") ?: ""
        val adminEmail = localProperties.getProperty("admin.email") ?: ""
        // Deployed yt-proxy origin. Precedence: -Pyt.proxy.base.url, then the
        // YT_PROXY_BASE_URL env var, then local.properties. The override means a
        // fresh deployment can be wired in without editing any file (CI, or
        // `YT_PROXY_BASE_URL=$(...) ./gradlew assembleDebug`).
        val ytProxyBaseUrl = (project.findProperty("yt.proxy.base.url") as String?)
            ?: System.getenv("YT_PROXY_BASE_URL")
            ?: localProperties.getProperty("yt.proxy.base.url")
            ?: ""

        buildConfigField("String", "B2_ACCESS_KEY_ID", "\"$b2AccessKeyId\"")
        buildConfigField("String", "B2_SECRET_ACCESS_KEY", "\"$b2SecretAccessKey\"")
        buildConfigField("String", "B2_BUCKET_NAME", "\"$b2BucketName\"")
        buildConfigField("String", "B2_ENDPOINT", "\"$b2Endpoint\"")
        buildConfigField("String", "ADMIN_EMAIL", "\"$adminEmail\"")
        // Blank until the yt-proxy is deployed; YtMusicApi.isConfigured reports it.
        buildConfigField("String", "YT_PROXY_BASE_URL", "\"$ytProxyBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Icons artifact is frozen upstream (no longer in newer Compose BOMs), so pin it.
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("com.amazonaws:aws-android-sdk-s3:2.81.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // YouTube Music proxy client (Stage 2)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Media3 for playback
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-datasource:1.11.1")

    // Google Cast SDK
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}