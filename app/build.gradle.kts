import java.util.Base64
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
        versionCode = (System.getenv("VERSION_CODE") ?: "3").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "10.0.0"
        // Developer support: set pronotes.upi / pronotes.upiName in gradle.properties
        // (or SUPPORT_UPI / SUPPORT_UPI_NAME in CI). Empty hides UPI payment button, while support messages remain visible.
        val upi = System.getenv("SUPPORT_UPI") ?: (project.findProperty("pronotes.upi") as String? ?: "")
        val upiName = System.getenv("SUPPORT_UPI_NAME") ?: (project.findProperty("pronotes.upiName") as String? ?: "Pro Notes")
        val cloudProjectId = System.getenv("CLOUD_PROJECT_ID") ?: (project.findProperty("pronotes.cloudProjectId") as String? ?: "")
        val cloudDbId = System.getenv("CLOUD_DB_ID") ?: (project.findProperty("pronotes.cloudDbId") as String? ?: "(default)")
        val cloudApiKey = System.getenv("CLOUD_API_KEY") ?: (project.findProperty("pronotes.cloudApiKey") as String? ?: "")
        val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: (project.findProperty("pronotes.googleClientId") as String? ?: "")
        buildConfigField("String", "SUPPORT_UPI", "\"$upi\"")
        buildConfigField("String", "SUPPORT_UPI_NAME", "\"$upiName\"")
        buildConfigField("String", "CLOUD_PROJECT_ID", "\"$cloudProjectId\"")
        buildConfigField("String", "CLOUD_DB_ID", "\"$cloudDbId\"")
        buildConfigField("String", "CLOUD_API_KEY", "\"$cloudApiKey\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
    }

    signingConfigs {
        val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
        if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storeType = "pkcs12"
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
        getByName("debug") {
            val userKs = file("${System.getProperty("user.home")}/.android/debug.keystore")
            val projectKs = rootProject.file("debug.keystore")
            val b64 = rootProject.file("debug.keystore.base64")
            if (!userKs.exists() && b64.exists()) {
                try {
                    userKs.parentFile?.mkdirs()
                    userKs.writeBytes(Base64.getDecoder().decode(b64.readText().trim()))
                } catch (_: Throwable) {}
            }
            if (userKs.exists()) {
                storeFile = userKs
            } else if (projectKs.exists()) {
                storeFile = projectKs
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
        buildConfig = true
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
    // Free on-device Gemini Nano (only on devices with Android AICore; the app falls back otherwise).
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
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
    // Official Kyant0 AndroidLiquidGlass Backdrop library for real-time optical refraction.
    implementation("io.github.kyant0:backdrop:1.0.6")
}
