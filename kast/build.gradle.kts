plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
}

val libName = "kast"
val libVersion = "0.1.0"
val artifact = "dev.datlag.kast"
group = artifact
version = libVersion

android {
    compileSdk = 34
    namespace = artifact

    defaultConfig {
        minSdk = 21
    }
    buildTypes {
        val debug by getting {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        val release by getting {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        publishAllLibraryVariants()
    }
    jvm()

    jvmToolchain(JavaVersion.VERSION_17.majorVersion.toIntOrNull() ?: (JavaVersion.VERSION_17.ordinal + 1))
    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidMain by getting {
            dependencies {
                api("com.google.android.gms:play-services-cast:21.3.0")
                api("com.google.android.gms:play-services-cast-framework:21.3.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
                implementation("org.jmdns:jmdns:3.5.8")
            }
        }
    }
}