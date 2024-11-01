# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# FFI layer
-keep class com.tari.android.wallet.ffi.** { *; }

# AIDL layer
-keep class com.tari.android.wallet.model.** { *; }

-keep class net.danlew.android.joda.R$raw { *; }

-keep enum yat.android.data.YatRecordType { *; }

-keep enum com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme { *; }

-keep enum com.tari.android.wallet.data.sharedPrefs.securityStages.WalletSecurityStage { *; }

-keep enum com.tari.android.wallet.application.Network { *; }

-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFResponse { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFsResponse { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.Data { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.Meta { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.Images { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.ImageVariant { *; }
-keep class com.tari.android.wallet.ui.common.gyphy.api.dto.Original { *; }

-keep class **Fragment** { *; }
-keep class **ViewModel** { *; }

-keep class com.tari.android.wallet.application.walletManager.WalletCallbackListener { *; }
