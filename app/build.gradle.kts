@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.android
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("io.sentry.android.gradle")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("download-libwallet")
}

android {
    namespace = "com.tari.android.wallet"

    defaultConfig {
        applicationId = "com.tari.android.wallet"
        minSdk = 26
        targetSdk = 34
        compileSdk = 35
        versionCode = BuildConfig.buildNumber
        versionName = "${BuildConfig.versionNumber}-libwallet-${BuildConfig.LibWallet.libwalletVersion}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_static")
            }
        }

        val yatProperties = loadYatProps()
        buildConfigField("String", "YAT_ORGANIZATION_NAME", "\"${yatProperties["yat.name"]}\"")
        buildConfigField("String", "YAT_ORGANIZATION_KEY", "\"${yatProperties["yat.key"]}\"")
        buildConfigField("String", "YAT_ORGANIZATION_RETURN_URL", "\"${yatProperties["yat.returnUrl"]}\"")

        val dropboxProperties = loadDropboxProps()
        buildConfigField("String", "DROPBOX_ACCESS_TOKEN", "\"${dropboxProperties["dropbox_key"]}\"")
        buildConfigField("String", "LIB_WALLET_MIN_VALID_VERSION", "\"${BuildConfig.LibWallet.libwalletMinValidVersion}\"")
    }

    flavorDimensions.add("privacy-mode")

    buildTypes {
        loadSentryProps()
        val secretProperties = loadSecretProps()

        getByName("debug") {
            isJniDebuggable = true
            buildConfigField("String", "GIPHY_KEY", "\"${secretProperties["giphy.key"]}\"")
        }

        getByName("release") {
            buildConfigField("String", "GIPHY_KEY", "\"${secretProperties["giphy.key"]}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
            sentry {
                autoUploadProguardMapping.set(true)
                uploadNativeSymbols.set(true)
                includeNativeSources.set(true)
            }
        }
    }

    productFlavors {
        create("regular") {
            dimension = "privacy-mode"
            buildConfigField("String", "NOTIFICATIONS_API_KEY", "\"${loadSecretProps()["service.notifications.api_key"]}\"")
            proguardFile("regular-proguard-rules.pro")
        }
        create("privacy") {
            dimension = "privacy-mode"
            buildConfigField("String", "NOTIFICATIONS_API_KEY", "\"${loadSecretProps()["service.notifications.api_key"]}\"")
        }
    }

    applicationVariants.configureEach {
        this.mergedFlavor.manifestPlaceholders["dropboxApiKey"] = loadDropboxProps()["dropbox_key"].toString()
        this.mergedFlavor.manifestPlaceholders["sentryPublicDSN"] = loadSecretProps()["sentry.public_dsn"].toString()
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                //added for exclude resources duplication
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/notice.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/INDEX.LIST",
                "META-INF/ASL2.0",
                "META-INF/LICENSE-notice.md",
                "META-INF/io.netty.versions.properties",
                "mozilla/public-suffix-list.txt",
                "xsd/catalog.xml",
                "com/itextpdf/io/font/cmap/HYSMyeongJoStd-Medium.properties",
                "com/itextpdf/io/font/cmap/HYGoThic-Medium.properties",
                "com/itextpdf/io/font/cmap/HYSMyeongJo-Medium.properties",
                "com/itextpdf/io/font/cmap/KozMinPro-Regular.properties",
                "com/itextpdf/io/font/cmap/MSungStd-Light.properties",
                "com/itextpdf/io/font/cmap_info.txt",
                "com/itextpdf/io/font/cmap/STSong-Light.properties",
                "com/itextpdf/io/font/cmap/STSongStd-Light.properties",
                "com/itextpdf/io/font/cmap/HeiseiKakuGo-W5.properties",
                "com/itextpdf/io/font/cmap/MSung-Light.properties",
                "com/itextpdf/io/font/cmap/MHei-Medium.properties",
                "com/itextpdf/io/font/cmap/HeiseiMin-W3.properties",
                "com/itextpdf/io/font/cmap/cjk_registry.properties"
            )
        }
    }

    lint {
        disable.addAll(
            listOf(
                "LintError", "NewApi", "TimberExceptionLogging", "LogNotTimber", "StringFormatInTimber",
                "ThrowableNotAtBeginning", "BinaryOperationInTimber", "TimberArgCount", "TimberArgTypes",
                "TimberTagLength"
            )
        )
    }

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.sun.activation:javax.activation:1.2.0")).using(module("jakarta.activation:jakarta.activation-api:1.2.1"))
            substitute(module("org.jetbrains.kotlin:kotlin-android-extensions-runtime:1.4.20")).using(module("org.jetbrains.kotlin:kotlin-android-extensions:1.6.21"))
            substitute(module("xpp3:xpp3:1.1.4c")).using(module("xml-apis:xml-apis:1.4.01"))
            substitute(module("xmlpull:xmlpull:1.1.3.1")).using(module("net.sf.kxml:kxml2:2.3.0"))
            substitute(module("org.jetbrains.kotlin:kotlin-build-common:1.6.21")).using(module("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.21"))
            substitute(module("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")).using(module("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.21"))
        }
    }
}

fun loadSecretProps(): Properties = loadProps("secret")
fun loadSentryProps(): Properties = loadProps("sentry")
fun loadYatProps(): Properties = loadProps("yat")
fun loadDropboxProps(): Properties = loadProps("dropbox")

fun loadProps(fileName: String): Properties {
    val props = project.rootProject.file("$fileName.properties")
    if (!props.exists()) {
        Files.copy(
            Paths.get(project.rootProject.file("$fileName-example.properties").absolutePath),
            Paths.get(props.absolutePath)
        )
    }
    val properties = Properties()
    properties.load(props.inputStream())
    return properties
}

// Comment this line if you want to disable the automatic download and use the native libraries of your choice
tasks.named("preBuild") { dependsOn("downloadLibwallet") }

dependencies {
    // It's recommended to use the latest version of the library from the JitPack repository,
    // but you can also use a locally build version of the library by using the ":yatlib" module and `yat-lib-debug-snapshot.aar` file with your build
    implementation("com.github.tari-project:yat-lib-android:0.5.0")
    // implementation project(":yatlib")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${BuildConfig.kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${BuildConfig.kotlinVersion}")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${BuildConfig.lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:${BuildConfig.lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${BuildConfig.lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${BuildConfig.lifecycleVersion}")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${BuildConfig.coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${BuildConfig.coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${BuildConfig.coroutinesVersion}")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.google.dagger:dagger:2.52")
    ksp("com.google.dagger:dagger-compiler:2.52")

    // debugImplementation because LeakCanary should only run in debug builds.
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // encryption
    implementation("com.github.adorsys:secure-storage-android:0.0.2")

    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.http-client:google-http-client-gson:1.45.1")
    implementation("com.google.api-client:google-api-client-android:2.7.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20241027-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation("net.danlew:android.joda:2.13.0")

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.orhanobut:logger:2.2.0")

    implementation("com.airbnb.android:lottie:6.6.0")

    // QR scanner
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")

    implementation("com.squareup:seismic:1.0.3")

    // sentry - crash analytics
    implementation("io.sentry:sentry-android:7.18.0")

    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // spring animation
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("com.github.MasayukiSuda:EasingInterpolator:1.3.2")

    implementation("info.guardianproject:tor-android:0.4.8.7")
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    // used to read log files
    implementation("commons-io:commons-io:2.18.0")

    implementation("com.dropbox.core:dropbox-core-sdk:5.4.6")

    implementation("com.github.vestrel00:contacts-android:0.3.1")

    implementation("com.github.weliem:blessed-android:2.5.0")

    implementation("com.giphy.sdk:ui:2.3.15") {
        exclude(group = "com.android.support")
    }

    implementation("com.itextpdf:itext7-core:9.0.0")

    implementation("net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:2.3.0")

    implementation("org.apache.maven:maven-artifact:3.9.9")

    // test
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation("io.mockk:mockk-android:1.13.13")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
