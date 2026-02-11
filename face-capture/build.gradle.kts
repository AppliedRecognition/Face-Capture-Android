import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.serializationJson)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.publish)
    signing
}

version = "2.2.0"

android {
    namespace = "com.appliedrec.verid3.facecapture"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    api(libs.verid.common)
    implementation(libs.verid.common.serialization)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.sceneview)
    implementation(libs.pager)
    implementation(libs.androidx.lifecycle.compose)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

mavenPublishing {
    coordinates("com.appliedrec", "verid-face-capture")
    pom {
        name.set("Face Capture")
        description.set("Captures live face")
        url.set("https://github.com/AppliedRecognition/Face-Capture-Android")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/AppliedRecognition/Face-Capture-Android.git")
            developerConnection.set("scm:git:ssh://github.com/AppliedRecognition/Face-Capture-Android.git")
            url.set("https://github.com/jakubdolejs/Face-Capture-Android")
        }
        developers {
            developer {
                id.set("appliedrecognition")
                name.set("Applied Recognition Corp.")
                email.set("support@appliedrecognition.com")
            }
        }
    }
    publishToMavenCentral(automaticRelease = true)
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.file("docs"))
    }
}

tasks.withType<DokkaTaskPartial>().configureEach {
    moduleName.set("Face capture")
    moduleVersion.set(project.version.toString())
}