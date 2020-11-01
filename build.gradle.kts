buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.0")
        classpath(kotlin("gradle-plugin", version = "1.4.10"))
        classpath("com.github.ben-manes:gradle-versions-plugin:0.29.0")

        classpath("com.google.gms:google-services:4.3.4")
        classpath("com.google.firebase:perf-plugin:1.3.3")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.3.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
