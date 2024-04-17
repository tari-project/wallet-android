<p align="center">
	<img width="300" src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/tari-logo.svg">
</p>


[![Actions Status](https://github.com/tari-project/wallet-android/workflows/Aurora%20Android%20Release%20Workflow/badge.svg)](https://github.com/tari-project/wallet-android/actions)

[![Actions Status](https://github.com/tari-project/wallet-android/workflows/PR%20Test%20Workflow/badge.svg)](https://github.com/tari-project/wallet-android/actions)

## What is Aurora?
Aurora is a reference-design mobile wallet app for the forthcoming [Tari](https://www.tari.com/) digital currency. The goal is for creators and developers to be able to use the open-source Aurora libraries and codebase as a starting point for developing their own Tari wallets and applications. Aurora also sets the bar for applications that use the Tari protocol. In its production-ready state, it will be a beautiful, easy to use Tari wallet focused on Tari as a default-private digital currency.

Want to contribute to Aurora? Get started here in this repository for Android or [here](https://github.com/tari-project/wallet-ios) for iOS.

<a href="https://play.google.com/store/apps/details?id=com.tari.android.wallet" target="_blank"><img width="100" src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/PlayStoreButton_large.svg"></a>&nbsp;&nbsp;&nbsp;<a href="https://apps.apple.com/us/app/tari-aurora/id1503654828" target="_blank"><img width="100" src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/AppStoreButton_large.svg"></a>

## Build Instructions

1. Install [Android Studio 4.0](https://developer.android.com/studio).
2. Clone the repository: `git clone https://github.com/tari-project/wallet-android.git`
3. Open Android Studio, select `Open an existing Android Studio Project` and select the project root folder.
4. Wait until Android Studio finishes initial downloading, syncing and indexing.
5. If you have not yet configured NDK, you should see the "NDK not configured" message at the end of the build and sync process. <p align="center"><img src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/01_NDK_Config.png"></p>
6. Click "Install NDK ..." link in the build log and accept the license, this will commence NDK and CMake installations. Let Android Studio do more downloading and sync.ing and wait until you see "CONFIGURE SUCCESSFUL" in the build logs. <p align="center"><img src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/02_Config_Successful.png"></p>
7. At this step, please go to the `SDK Manager` and make sure that both NDK and CMake are installed. <p align="center"><img src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/03_NDK_and_CMake_Installed.png"></p>
8. Go to `Build Variants` and select `regularDebug` for the full configuration, or `privacyDebug` for privacy configuration. <p align="center"><img src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/04_Build_Config.png"></p>
9. Make project. (`Build` → `Make Project`)
10. `OPTIONAL` `secret.properties` file in the project folder contains application secrets and will be generated for you with default values during the build process. Please follow the comments in this file and edit if required.
11. `OPTIONAL` The regular build of Aurora uses [Sentry](https://sentry.io/) for error monitoring. `sentry.properties` file in the project folder contains Sentry configuration and will be created with empty values for you during the build process. Please follow the comments in this file and edit with your Sentry server values if you'd like to enable crash reporting.
12. **_Voila_** 🎉 You're now ready to run Aurora! Please follow the next item on how to setup your emulator, or you can already run it on your device.

### Emulator Configuration

Aurora Android native libraries only support `armeabi-v7a`, `arm64-v8a` and `x86_64` [ABIs](https://developer.android.com/ndk/guides/abis). Therefore you can only run Aurora on devices and emulators with a supported ABI. Android Studio emulators use the unsupported `x86` ABI as default, but you can download an `x86_64` image for your emulator and run Aurora on it. Please `Download` an `x86_64` image under the `x86 Images` tab in the `System Image` step of `Virtual Device Configuration.` <p align="center"><img src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/05_x86_64.png"></p>

### Using Your Custom Native Library (FFI) Version

Tari native wallet library version to be used by Aurora is specified by the `ext.libwalletVersion` in the `build.gradle` file in the project root directory. Aurora build script automatically fetches the native libraries of this version into the `libwallet` folder in the root directory during the build process. Any library files you place in this folder will be deleted and replaced by this automatic download process.

If you want to disable the automatic download and use the native libraries of your choice, please comment out the line `preBuild.dependsOn("downloadLibwallet")` in the file `app/build.gradle`. 

### For updating openssl
https://github.com/217heidai/openssl_for_android/releases
