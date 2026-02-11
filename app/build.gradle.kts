import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    kotlin("kapt")
    alias(libs.plugins.serializationJson)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.appliedrec.verid3.facecapturedemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appliedrec.verid3.facecapturedemo"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        jniLibs {
            pickFirsts.add("lib/arm64-v8a/libonnxruntime.so")
            pickFirsts.add("lib/x86_64/libonnxruntime.so")
            pickFirsts.add("lib/armeabi-v7a/libonnxruntime.so")
            pickFirsts.add("lib/x86/libonnxruntime.so")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)
    implementation(project(":face-capture"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.face.detection.retinaface)
    implementation(libs.spoof.device.detection)
    implementation(libs.materialIconsExtended)
    implementation(libs.systemUiController)
    implementation(libs.video.recording.plugin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kapt {   correctErrorTypes = true }