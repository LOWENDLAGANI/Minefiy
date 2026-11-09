plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.minetallest.minefiy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.minetallest.minefiy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val b2AccessKeyId = localProperties.getProperty("b2.access.key.id") ?: ""
        val b2SecretAccessKey = localProperties.getProperty("b2.secret.access.key") ?: ""
        val b2BucketName = localProperties.getProperty("b2.bucket.name") ?: ""
        val b2Endpoint = localProperties.getProperty("b2.endpoint") ?: ""
        val adminEmail = localProperties.getProperty("admin.email") ?: ""

        buildConfigField("String", "B2_ACCESS_KEY_ID", "\"$b2AccessKeyId\"")
        buildConfigField("String", "B2_SECRET_ACCESS_KEY", "\"$b2SecretAccessKey\"")
        buildConfigField("String", "B2_BUCKET_NAME", "\"$b2BucketName\"")
        buildConfigField("String", "B2_ENDPOINT", "\"$b2Endpoint\"")
        buildConfigField("String", "ADMIN_EMAIL", "\"$adminEmail\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("com.amazonaws:aws-android-sdk-s3:2.81.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Media3 for playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-datasource:1.3.1")

    // Google Cast SDK
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}