/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")
            force("com.squareup:javapoet:1.13.0")
        }
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.benManes) apply true
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.arturboschDetekt) apply false
    alias(libs.plugins.googleHilt) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.devtoolsKsp) apply false
}

fun isNonStable(version: String): Boolean {
    return version.contains("alpha", true) ||
            version.contains("beta", true) ||
            version.contains("dev", true)
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

subprojects {
    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion("2.1.10")
                }
                if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
                    useVersion("2.3.20")
                }
//                if (requested.group == "androidx.room") {
//                    useVersion("2.6.1")
//                }
            }
            force("com.squareup:javapoet:1.13.0")
        }
    }

    // Force Kotlin version for KSP and other processing tools
    configurations.matching { it.name.contains("ksp", ignoreCase = true) || it.name.contains("annotationProcessor", ignoreCase = true) }.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion("2.1.10")
                }
                if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
                    useVersion("2.3.20")
                }
                if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-compiler-embeddable") {
                    useVersion("2.1.10")
                }
            }
            force("com.squareup:javapoet:1.13.0")
        }
    }
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll("-Xjdk-release=21", "-Xskip-metadata-version-check")
        }
    }
}
