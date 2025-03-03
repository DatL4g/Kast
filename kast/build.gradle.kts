import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    id("com.android.library")
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.serialization)
    alias(libs.plugins.vanniktech.publish)
    `maven-publish`
    signing
}

val libName = "kast"
val libVersion = "0.2.1"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        publishAllLibraryVariants()
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Chromecast library"
        ios.deploymentTarget = "14.0"

        framework {
            baseName = "Kast"
        }

        pod(name = "google-cast-sdk", moduleName = "GoogleCast")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines)
                implementation(libs.immutable)
                implementation(libs.serialization)
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.mediarouter)
                api(libs.play.services.cast)
                api(libs.play.services.cast.framework)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.0")
                implementation("org.jmdns:jmdns:3.5.8")
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(host = SonatypeHost.S01, automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = artifact,
        artifactId = libName,
        version = libVersion
    )

    pom {
        name.set(libName)
        description.set("Kotlin multiplatform casting library.")
        url.set("https://github.com/DatL4g/Kast")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        scm {
            url.set("https://github.com/DatL4g/Kast")
            connection.set("scm:git:git://github.com/DatL4g/Kast.git")
        }

        developers {
            developer {
                id.set("DatL4g")
                name.set("Jeff Retz (DatLag)")
                url.set("https://github.com/DatL4g")
            }
        }
    }
}