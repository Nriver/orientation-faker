import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("com.github.ben-manes.versions")
}

val applicationName = "OrientationFaker"
val versionMajor = 4
val versionMinor = 2
val versionPatch = 8

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.3"

    defaultConfig {
        applicationId = "net.mm2d.android.orientationfaker"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        resConfigs("en", "ja")
        vectorDrawables.useSupportLibrary = true
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        base.archivesBaseName = "${applicationName}-${versionName}"
        multiDexEnabled = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "d"
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as BaseVariantOutputImpl).outputFileName = "${applicationName}-${versionName}.apk"
            }
        }
    }
}

val kotlinVersion: String by project

dependencies {
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.3.1")
    implementation("androidx.browser:browser:1.2.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.lifecycle:lifecycle-process:2.2.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.google.android.material:material:1.2.0")
    implementation("com.google.android.play:core:1.8.0")
    implementation("com.google.android.play:core-ktx:1.8.0")
    implementation("androidx.room:room-runtime:2.2.5")
    implementation("androidx.room:room-ktx:2.2.5")
    kapt("androidx.room:room-compiler:2.2.5")
    implementation("net.mm2d:log:0.9.1")
    implementation("net.mm2d:log-android:0.9.1")
    implementation("net.mm2d:color-chooser:0.1.6")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.8")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.4")
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}
