import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    id("org.jetbrains.kotlin.plugin.compose") version Dependencies.Kotlin.version
    id("com.google.gms.google-services")
}

val commitNumber: Int by lazy {
    val processBuilder = ProcessBuilder("git", "rev-list", "--count", "HEAD")
    val output = File.createTempFile("git-commit-count", "")
    processBuilder.redirectOutput(output)
    val process = processBuilder.start()
    process.waitFor()
    output.readText().trim().toInt()
}

android {
    namespace = "com.tari.android.wallet"

    defaultConfig {
        applicationId = "com.tari.android.wallet"
        minSdk = TariBuildConfig.minSdk
        targetSdk = TariBuildConfig.targetSdk
        compileSdk = TariBuildConfig.compileSdk
        versionCode = commitNumber
        versionName = "${TariBuildConfig.versionNumber}-libwallet-${TariBuildConfig.LibWallet.version}"
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

        buildConfigField("String", "LIB_WALLET_MIN_VALID_VERSION", "\"${TariBuildConfig.LibWallet.minValidVersion}\"")
        buildConfigField("String", "LIB_WALLET_VERSION", "\"${TariBuildConfig.LibWallet.version}\"")
        buildConfigField("String", "LIB_WALLET_NETWORK", "\"${TariBuildConfig.LibWallet.network}\"")

        buildConfigField("String", "NOTIFICATIONS_API_KEY", "\"${loadSecretProps()["service.notifications.api_key"]}\"")
    }

    flavorDimensions.add("privacy-mode")

    buildTypes {
        loadSentryProps()

        getByName("debug") {
            isJniDebuggable = true
        }

        getByName("release") {
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
        }
        create("privacy") {
            dimension = "privacy-mode"
        }
    }

    applicationVariants.configureEach {
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

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
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
                "com/itextpdf/io/font/cmap/cjk_registry.properties",
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

// Remove the "downloadLibwallet" task if you want to disable the automatic download and use the native libraries of your choice
tasks.named("preBuild") { dependsOn("downloadLibwallet") }

dependencies {
    // It's recommended to use the latest version of the library from the JitPack repository,
    // but you can also use a locally build version of the library by using the ":yatlib" module and `yat-lib-debug-snapshot.aar` file with your build
    implementation(Dependencies.yatLibAndroid)
    // implementation project(":yatlib")

    implementation(Dependencies.Kotlin.reflect)
    implementation(Dependencies.Kotlin.stdlib)

    implementation(Dependencies.AndroidX.appcompat)
    implementation(Dependencies.AndroidX.biometric)
    implementation(Dependencies.AndroidX.constraintLayout)
    implementation(Dependencies.AndroidX.coreKtx)
    implementation(Dependencies.AndroidX.legacySupport)
    implementation(Dependencies.AndroidX.recyclerview)
    implementation(Dependencies.AndroidX.viewpager2)
    implementation(Dependencies.AndroidX.material)
    implementation(Dependencies.AndroidX.activityKtx)
    implementation(Dependencies.AndroidX.fragmentKtx)
    implementation(Dependencies.AndroidX.Lifecycle.extensions)
    implementation(Dependencies.AndroidX.Lifecycle.livedataKtx)
    implementation(Dependencies.AndroidX.Lifecycle.reactivestreamsKtx)
    implementation(Dependencies.AndroidX.Lifecycle.runtimeKtx)
    implementation(Dependencies.AndroidX.Lifecycle.viewmodelKtx)
    implementation(platform(Dependencies.AndroidX.Compose.bom))
    implementation(Dependencies.AndroidX.Compose.material)
    implementation(Dependencies.AndroidX.Compose.material3)
    implementation(Dependencies.AndroidX.Compose.uiTooling)
    implementation(Dependencies.AndroidX.Compose.uiToolingPreview)
    implementation(Dependencies.AndroidX.Compose.activity)

    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)

    implementation(Dependencies.flexbox)

    implementation(Dependencies.dagger)
    ksp(Dependencies.daggerCompiler)

    // debugImplementation because LeakCanary should only run in debug builds.
    debugImplementation(Dependencies.leakCanary)

    // encryption
    implementation(Dependencies.secureStorage)

    implementation(platform(Dependencies.Firebase.bom))
    implementation(Dependencies.Firebase.messaging)

    implementation(Dependencies.Google.playServicesAuth)
    implementation(Dependencies.Google.httpClientGson)
    implementation(Dependencies.Google.apiClientAndroid) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(Dependencies.Google.apiServicesDrive) {
        exclude(group = "org.apache.httpcomponents")
    }

    implementation(Dependencies.jodaTime)

    implementation(Dependencies.gson)

    implementation(Dependencies.logger)

    implementation(Dependencies.lottie)

    // QR scanner
    implementation(Dependencies.codeScanner)
    implementation(Dependencies.zxingAndroidEmbedded)

    implementation(Dependencies.Retrofit.retrofit)
    implementation(Dependencies.Retrofit.converterGson)
    implementation(Dependencies.Retrofit.okhttpLoggingInterceptor)

    implementation(Dependencies.seismic)

    // sentry - crash analytics
    implementation(Dependencies.Sentry.sentryAndroid)

    implementation(Dependencies.rxjava)

    // spring animation
    implementation(Dependencies.AndroidX.dynamicAnimation)
    implementation(Dependencies.easingInterpolator)

    // used to read log files
    implementation(Dependencies.commonsIo)

    implementation(Dependencies.keyboardVisibilityEvent)

    implementation(Dependencies.mavenArtifact)

    implementation(Dependencies.paperDb)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.mockk)
    androidTestImplementation(Dependencies.Test.mockkAndroid)
    androidTestImplementation(Dependencies.Test.testCore)
    androidTestImplementation(Dependencies.Test.testExtJunit)
    androidTestImplementation(Dependencies.Test.espressoCore)
}
