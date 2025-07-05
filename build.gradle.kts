buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(Dependencies.gradlePlugin)
        classpath(Dependencies.Kotlin.gradlePlugin)
        classpath(Dependencies.Sentry.gradlePlugin)
        classpath(Dependencies.Firebase.gradlePlugin)
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
