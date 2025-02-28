object Dependencies {
    const val gradlePlugin = "com.android.tools.build:gradle:8.7.2"

    object Kotlin {
        const val version = "2.0.21"

        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    }

    object AndroidX {
        const val appcompat = "androidx.appcompat:appcompat:1.7.0"
        const val biometric = "androidx.biometric:biometric:1.1.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.2.0"
        const val coreKtx = "androidx.core:core-ktx:1.15.0"
        const val legacySupport = "androidx.legacy:legacy-support-v13:1.0.0"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.3.2"
        const val viewpager2 = "androidx.viewpager2:viewpager2:1.1.0"
        const val activityKtx = "androidx.activity:activity-ktx:1.9.3"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:1.8.5"
        const val dynamicAnimation = "androidx.dynamicanimation:dynamicanimation:1.0.0"

        object Lifecycle {
            const val version = "2.8.7"

            const val extensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
            const val livedataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val reactivestreamsKtx = "androidx.lifecycle:lifecycle-reactivestreams-ktx:$version"
            const val runtimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
        }

        object Compose {
            const val bom = "androidx.compose:compose-bom:2025.02.00"
            const val material = "androidx.compose.material:material"
            const val uiTooling = "androidx.compose.ui:ui-tooling"
            const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
            const val activity = "androidx.activity:activity-compose:1.9.2"
        }
    }

    object Coroutines {
        const val version = "1.9.0"

        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val rx2 = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$version"
    }

    object Firebase {
        const val gradlePlugin = "com.google.gms:google-services:4.4.2"

        const val bom = "com.google.firebase:firebase-bom:33.8.0"
        const val messaging = "com.google.firebase:firebase-messaging"
    }

    object Retrofit {
        const val retrofit = "com.squareup.retrofit2:retrofit:2.11.0"
        const val converterGson = "com.squareup.retrofit2:converter-gson:2.11.0"
        const val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14"
    }

    object Google {
        const val playServicesAuth = "com.google.android.gms:play-services-auth:21.2.0"
        const val httpClientGson = "com.google.http-client:google-http-client-gson:1.45.1"
        const val apiClientAndroid = "com.google.api-client:google-api-client-android:2.7.0"
        const val apiServicesDrive = "com.google.apis:google-api-services-drive:v3-rev20241027-2.0.0"
    }

    object Test {
        const val junit = "junit:junit:4.13.2"
        const val mockk = "io.mockk:mockk:1.13.13"
        const val mockkAndroid = "io.mockk:mockk-android:1.13.13"
        const val testCore = "androidx.test:core:1.6.1"
        const val testExtJunit = "androidx.test.ext:junit:1.2.1"
        const val espressoCore = "androidx.test.espresso:espresso-core:3.6.1"
    }

    object Sentry {
        const val gradlePlugin = "io.sentry:sentry-android-gradle-plugin:4.14.0"
        const val sentryAndroid = "io.sentry:sentry-android:7.18.0"
    }

    const val yatLibAndroid = "com.github.tari-project:yat-lib-android:0.5.0"
    const val flexbox = "com.google.android.flexbox:flexbox:3.0.0"
    const val glide = "com.github.bumptech.glide:glide:4.16.0"
    const val glideCompiler = "com.github.bumptech.glide:compiler:4.16.0"
    const val dagger = "com.google.dagger:dagger:2.52"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:2.52"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.14"
    const val secureStorage = "com.github.adorsys:secure-storage-android:0.0.2"
    const val jodaTime = "net.danlew:android.joda:2.13.0"
    const val gson = "com.google.code.gson:gson:2.11.0"
    const val logger = "com.orhanobut:logger:2.2.0"
    const val lottie = "com.airbnb.android:lottie:6.6.0"
    const val codeScanner = "com.github.yuriy-budiyev:code-scanner:2.3.2"
    const val zxingAndroidEmbedded = "com.journeyapps:zxing-android-embedded:4.3.0"
    const val seismic = "com.squareup:seismic:1.0.3"
    const val rxandroid = "io.reactivex.rxjava2:rxandroid:2.1.1"
    const val rxjava = "io.reactivex.rxjava2:rxjava:2.2.21"
    const val easingInterpolator = "com.github.MasayukiSuda:EasingInterpolator:1.3.2"
    const val torAndroid = "info.guardianproject:tor-android:0.4.8.7"
    const val jtorctl = "info.guardianproject:jtorctl:0.4.5.7"
    const val commonsIo = "commons-io:commons-io:2.18.0"
    const val dropboxCoreSdk = "com.dropbox.core:dropbox-core-sdk:5.4.6"
    const val contactsAndroid = "com.github.vestrel00:contacts-android:0.3.1"
    const val blessedAndroid = "com.github.weliem:blessed-android:2.5.0"
    const val giphySdkUi = "com.giphy.sdk:ui:2.3.15"
    const val itext7Core = "com.itextpdf:itext7-core:9.0.0"
    const val keyboardVisibilityEvent = "net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:2.3.0"
    const val mavenArtifact = "org.apache.maven:maven-artifact:3.9.9"
}