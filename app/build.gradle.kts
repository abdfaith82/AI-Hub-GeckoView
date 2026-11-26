plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.abd.aihubgecko"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abd.aihubgecko"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // GeckoView — محرك موزيلا. الإصدار 141.0 (آب 2025) هو أحدث
    // إصدار يقبل compileSdk 35: الإصدار 161.0 يشترط compileSdk 37
    // (minCompileSdk=37 في بيانات الـ AAR) فيفشل البناء مع AGP 8.6.
    // وهو ما يتيح الإضافة المدمجة التي تقرأ نص الصفحات — وهو أيضاً
    // سبب كبر حجم الـ APK (~150 ميغا).
    implementation("org.mozilla.geckoview:geckoview:141.0.20250806102122")
}
