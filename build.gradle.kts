buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${BuildConfig.agpVersion}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildConfig.kotlinVersion}")
        classpath("io.sentry:sentry-android-gradle-plugin:4.14.0")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
