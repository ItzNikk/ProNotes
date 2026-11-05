import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.pronotes.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pronotes.app"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
        if (!ksPath.isNullOrBlank()) {
            create("release") {
                storeFile = file(ksPath)
                storeType = "pkcs12"
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Jetpack Ink: low-latency stylus strokes, brushes, geometry (eraser hit tests), rendering, storage.
    val ink = "1.0.0"
    implementation("androidx.ink:ink-authoring:$ink")
    implementation("androidx.ink:ink-brush:$ink")
    implementation("androidx.ink:ink-geometry:$ink")
    implementation("androidx.ink:ink-rendering:$ink")
    implementation("androidx.ink:ink-strokes:$ink")
    implementation("androidx.ink:ink-storage:$ink")
    // Motion prediction lowers perceived pen latency.
    implementation("androidx.input:input-motionprediction:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    // Drag-to-reorder lists (pages panel, toolbar customisation).
    implementation("sh.calvin.reorderable:reorderable:2.4.3")
    // Rounded polygons, stars and blobs for the shape tool.
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    // Colour wheel + brightness slider for custom colours.
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    // PDF text extraction for in-document search.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
