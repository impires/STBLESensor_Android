/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.arturboschDetekt)
    alias(libs.plugins.googleHilt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.devtoolsKsp)
    alias(libs.plugins.jaredsburrowsLicense)
}

apply(from = "st_dependencies.gradle")

android {
    namespace = "com.st.bluems"
    // Use the fetched variables
    compileSdk = (rootProject.findProperty("stCompileSdk")?.toString() ?: "36").toInt()

    defaultConfig {
        applicationId = "com.st.bluems.custom"
        minSdk = (rootProject.findProperty("stMinSdk")?.toString() ?: "26").toInt()
        targetSdk = (rootProject.findProperty("stTargetSdk")?.toString() ?: "36").toInt()
        versionCode = 365
        versionName = "5.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        manifestPlaceholders["appAuthRedirectScheme"] = "stblesensor"

        buildConfigField(
            type = "String",
            name = "VESPUCCI_ENVIRONMENT",
            value = "\"PROD\"" // "\"PRE_PROD\" ""\"DEV\""
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

hilt {
    enableAggregatingTask = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

licenseReport {
    // Generate reports
    generateCsvReport = false
    generateHtmlReport = true
    generateJsonReport = false
    generateTextReport = false

    // Copy reports - These options are ignored for Java projects
    copyCsvReportToAssets = false
    copyHtmlReportToAssets = true
    copyJsonReportToAssets = false
    copyTextReportToAssets = false
    useVariantSpecificAssetDirs = false

    // Ignore licenses for certain artifact patterns
    //ignoredPatterns = []

    // Show versions in the report - default is false
    showVersions = true
}

detekt {
    config.setFrom("../detekt-config-compose.yml")
}

dependencies {
    // Blue ST module:
    // - Core
    implementation(project(":st_core"))
    // - UI
    implementation(project(":st_ui"))
    // - Preferences
    implementation(project(":st_preferences"))
    // - User Profiling
    implementation(project(":st_user_profiling"))
    // - Welcome
    implementation(project(":st_welcome"))
    // - Licenses
    implementation(project(":st_licenses"))
    // - Terms
    implementation(project(":st_terms"))
    // - Demos
    implementation(project(":st_demo_showcase"))
    // - Discover Catalog
    implementation(project(":st_catalog"))
    // - Login
    implementation(project(":st_login"))

    implementation(project(":st_multinode"))

    // Blue ST SDK
    implementation(libs.st.sdk) {
        exclude(group = "androidx.room", module = "room-runtime")
        exclude(group = "androidx.room", module = "room-ktx")
        exclude(group = "androidx.room", module = "room-common")
        exclude(group = "androidx.room", module = "room-paging")
        exclude(group = "androidx.room", module = "room-compiler")
    }

    // Room
    val roomVersion = "2.8.4"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("androidx.room:room-testing:$roomVersion")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata)

    // Dependency required for API desugaring.
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.uitestmanifest)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.test)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.room") {
            useVersion("2.8.4")
        }
    }
}


