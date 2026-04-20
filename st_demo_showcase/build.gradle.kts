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
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleHilt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.devtoolsKsp)
    
    alias(libs.plugins.kotlinSerialization)
}

apply(from = "publish.gradle")
android {
    namespace = "com.st.demo_showcase"

    // Use the fetched variables
    compileSdk = stCompileSdk

    defaultConfig {
        minSdk = stMinSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        compose = true
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // Blue ST module:
    // - UI
    implementation(project(":st_ui"))
    // - Core
    implementation(project(":st_core"))
    // - Prefs
    implementation(project(":st_preferences"))
    // - Login
    implementation(project(":st_login"))
    // - User Profiling
    implementation(project(":st_user_profiling"))

    // - Demos
    implementation(project(":st_compass"))
    implementation(project(":st_level"))
    implementation(project(":st_fitness"))
    implementation(project(":st_environmental"))
    implementation(project(":st_high_speed_data_log"))
    implementation(project(":st_blue_voice"))
    implementation(project(":st_gesture_navigation"))
    implementation(project(":st_neai_anomaly_detection"))
    implementation(project(":st_neai_classification"))
    implementation(project(":st_event_counter"))
    implementation(project(":st_piano"))
    implementation(project(":st_pnpl"))
    implementation(project(":st_plot"))
    implementation(project(":st_nfc_writing"))
    implementation(project(":st_binary_content"))
    implementation(project(":st_ext_config"))
    implementation(project(":st_tof_objects_detection"))
    implementation(project(":st_color_ambient_light"))
    implementation(project(":st_gnss"))
    implementation(project(":st_motion_intensity"))
    implementation(project(":st_activity_recognition"))
    implementation(project(":st_carry_position"))
    implementation(project(":st_mems_gesture"))
    implementation(project(":st_motion_algorithms"))
    implementation(project(":st_pedometer"))
    implementation(project(":st_proximity_gesture_recognition"))
    implementation(project(":st_switch_demo"))
    implementation(project(":st_registers_demo"))
    implementation(project(":st_acceleration_event"))
    implementation(project(":st_source_localization"))
    implementation(project(":st_audio_classification_demo"))
    implementation(project(":st_led_control"))
    implementation(project(":st_node_status"))
    implementation(project(":st_textual_monitor"))
    implementation(project(":st_heart_rate_demo"))
    implementation(project(":st_sensor_fusion"))
    implementation(project(":st_predicted_maintenance"))
    implementation(project(":st_fft_amplitude"))
    implementation(project(":st_multi_neural_network"))
    implementation(project(":st_flow_demo"))
    implementation(project(":st_raw_pnpl"))
    implementation(project(":st_smart_motor_control"))
    implementation(project(":st_cloud_azure_iot_central"))
    implementation(project(":st_cloud_mqtt"))
    implementation(project(":st_neai_extrapolation"))
    implementation(project(":st_medical_signal"))
    implementation(project(":st_asset_tracking_event"))
    implementation(project(":st_external_app"))

    // Blue ST SDK
    implementation(libs.st.sdk)

    // Reorderable
    implementation(libs.reorderable)

    // Hilt
    implementation(libs.hilt.android)
    implementation(project(":st_multinode"))

    ksp(libs.hilt.compiler)

    // Dependency required for API desugaring.

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.test)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
