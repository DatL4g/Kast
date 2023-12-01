import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("com.android.library") version "8.1.4" apply false
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
    id("com.vanniktech.maven.publish") version "0.25.3" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
}
