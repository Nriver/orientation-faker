buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath(kotlin("gradle-plugin", version = "1.5.20"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.39.0")

        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:perf-plugin:1.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
