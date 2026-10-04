import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Secrets live in local.properties (git-ignored). See local.properties.example.
// NEVER commit real keys.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(::load)
}
fun prop(name: String, default: String): String =
    localProps.getProperty(name, default) ?: default

android {
    namespace = "com.techinfotics.kodalang"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.techinfotics.kodalang"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${prop("SUPABASE_URL", "https://tsvfdzkelmznxqlcvrjb.supabase.co")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${prop("SUPABASE_ANON_KEY", "")}\""
        )
        // Base URL of the KodaLang web app — hosts /api/tutor-chat and /api/tutor-tts.
        buildConfigField(
            "String", "API_BASE",
            "\"${prop("API_BASE", "https://kodalang.com")}\""
        )
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Supabase (auth + postgrest). Versions verified on Maven Central.
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // HTTP client for the kodalang.com /api/* AI routes.
    implementation(libs.okhttp)

    // Custom Tabs for honest deep-links (Goal / Memory Health / Exam).
    implementation(libs.androidx.browser)
}
