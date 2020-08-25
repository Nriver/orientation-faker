import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("com.github.ben-manes.versions")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

val applicationName = "OrientationFaker"
val versionMajor = 4
val versionMinor = 2
val versionPatch = 10

android {
    compileSdkVersion(29)

    defaultConfig {
        applicationId = "net.mm2d.android.orientationfaker"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
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
                (this as BaseVariantOutputImpl).outputFileName = "${applicationName}-${versionName}-google-play.apk"
            }
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.3.1")
    implementation("androidx.browser:browser:1.2.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.lifecycle:lifecycle-process:2.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.google.android.material:material:1.2.0")
    implementation("com.google.android.play:core:1.8.0")
    implementation("com.google.android.play:core-ktx:1.8.1")
    implementation("androidx.room:room-runtime:2.2.5")
    implementation("androidx.room:room-ktx:2.2.5")
    kapt("androidx.room:room-compiler:2.2.5")
    implementation("net.mm2d:log:0.9.2")
    implementation("net.mm2d:log-android:0.9.2")
    implementation("net.mm2d:color-chooser:0.1.8")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.4")

    implementation("com.google.firebase:firebase-core:17.5.0")
    implementation("com.google.firebase:firebase-ads:19.3.0")
    implementation("com.google.firebase:firebase-perf:19.0.8")
    implementation("com.google.firebase:firebase-crashlytics:17.2.1")
    implementation("com.google.firebase:firebase-analytics:17.5.0")
    implementation("com.google.android.ads.consent:consent-library:1.0.8")
}

fun isStable(version: String): Boolean {
    val versionUpperCase = version.toUpperCase()
    val hasStableKeyword = listOf("RELEASE", "FINAL", "GA").any { versionUpperCase.contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return hasStableKeyword || regex.matches(version)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf { !isStable(candidate.version) }
}

apply(plugin = "com.google.gms.google-services")
