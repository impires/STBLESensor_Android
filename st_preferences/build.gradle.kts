/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */

val stCompileSdk: Int = (rootProject.findProperty("stCompileSdk") as String).toInt()
val stMinSdk: Int = (rootProject.findProperty("stMinSdk") as String).toInt()
val stTargetSdk: Int = (rootProject.findProperty("stTargetSdk") as String).toInt()

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.googleHilt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.devtoolsKsp)
}

apply(from = "publish.gradle")

android {
    namespace = "com.st.preferences"

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
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    // Blue ST module:
    // - Core
    implementation(project(":st_core"))
    // - UI
    implementation(project(":st_ui"))

    // Blue ST SDK
    implementation(libs.st.sdk)

    // Datastore
    implementation(libs.androidx.datastore)

    // Hilt
    implementation(libs.hilt.android)
    
    ksp(libs.hilt.compiler)

    // Dependency required for API desugaring.

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.test)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
