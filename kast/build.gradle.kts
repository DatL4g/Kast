import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("com.vanniktech.maven.publish")
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
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0")
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.mediarouter:mediarouter:1.7.0")
                api("com.google.android.gms:play-services-cast:21.5.0")
                api("com.google.android.gms:play-services-cast-framework:21.5.0")
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