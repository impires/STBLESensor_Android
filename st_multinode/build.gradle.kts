plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.arturboschDetekt)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.devtoolsKsp)
    alias(libs.plugins.googleHilt)
    alias(libs.plugins.jaredsburrowsLicense)
}

val stCompileSdk: Int = (rootProject.findProperty("stCompileSdk") as String).toInt()
val stMinSdk: Int = (rootProject.findProperty("stMinSdk") as String).toInt()

android {
    namespace = "com.st.multinode"
    // Use the fetched variables
    compileSdk = stCompileSdk

    defaultConfig {
        minSdk = stMinSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "stblesensor"
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.st.sdk)
    implementation(project(":st_core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}