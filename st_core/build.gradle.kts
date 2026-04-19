/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */

val stCompileSdk: Int = libs.versions.stCompileSdk.get().toInt()
val stMinSdk: Int = libs.versions.stMinSdk.get().toInt()
val stTargetSdk: Int = libs.versions.stTargetSdk.get().toInt()

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.googleHilt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.devtoolsKsp)
}

apply(from = "publish.gradle")

android {
    namespace = "com.st.core"

    // Use the fetched variables
    compileSdk = stCompileSdk

    defaultConfig {
        minSdk = stMinSdk

        manifestPlaceholders["appAuthRedirectScheme"] = "stblesensor"

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    // Blue ST SDK
    implementation(libs.st.sdk)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Hilt
    implementation(libs.hilt.android)
    
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)

    // Coroutines
    api(libs.kotlinx.coroutines.core)

    // Compat
    api(libs.bundles.compat)

    // KTX
    api(libs.bundles.ktx)

    // Retrofit
    api(libs.bundles.network)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.test)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Dependency required for API desugaring.
}
