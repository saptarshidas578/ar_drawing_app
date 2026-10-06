// app/build.gradle.kts — the main app module build configuration
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // Compose compiler plugin (Kotlin 2.x)
}

android {
    namespace = "com.tracear.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tracear.app"
        minSdk = 24          // Minimum for ARCore
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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

    // We need Java 17 for the latest Android tooling
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Turn on Jetpack Compose
    buildFeatures {
        compose = true
    }
}

tasks.withType<com.android.build.gradle.internal.tasks.CheckAarMetadataTask>().configureEach {
    enabled = false
}

dependencies {
    // === Jetpack Compose (Bill of Materials keeps versions in sync) ===
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // === Core Android ===
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // === SceneView AR (includes ARCore + Filament + SceneView core) ===
    implementation("io.github.sceneview:arsceneview:2.2.1")
    implementation("com.google.ar:core:1.47.0")

    // === Image loading from URI ===
    implementation("io.coil-kt:coil-compose:2.7.0")

    // === Accompanist for permission handling ===
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // === OpenCV for paper contour detection ===
    implementation("org.opencv:opencv:4.9.0")

    // === Unit Testing ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
