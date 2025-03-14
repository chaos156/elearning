plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.elearning"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.elearning"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.functions.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.firebase.auth.ktx)
    implementation (libs.firebase.firestore.ktx)
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx.v287)
    implementation(libs.androidx.activity.compose.v1101)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.v178)
    implementation(libs.ui.tooling.preview)
    implementation (libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    implementation (libs.material)

    implementation (libs.google.firebase.auth.ktx)
    implementation (libs.google.firebase.firestore.ktx)
    implementation (libs.material3)
    implementation(libs.coil.compose)
    implementation (libs.androidx.ui.text)
    implementation (libs.androidx.material.v178)
    implementation ("com.google.api-client:google-api-client-android:1.35.0")
    implementation ("com.google.oauth-client:google-oauth-client-android:1.35.0")
    implementation ("com.google.http-client:google-http-client-gson:1.39.2")
    implementation ("com.google.apis:google-api-services-gmail:v1-rev305-1.25.0")
// Check the latest BOM version
    implementation ("com.google.android.gms:play-services-auth:20.0.1")

}