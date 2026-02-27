<p align="center">
	<img width="300" src="https://raw.githubusercontent.com/tari-project/wallet-android/development/readme-files/tari-logo.svg">
</p>

[![Aurora Android Release Workflow](https://github.com/tari-project/wallet-android/workflows/Aurora%20Android%20Release%20Workflow/badge.svg)](https://github.com/tari-project/wallet-android/actions)
[![PR Test Workflow](https://github.com/tari-project/wallet-android/workflows/PR%20Test%20Workflow/badge.svg)](https://github.com/tari-project/wallet-android/actions)

## What is Tari Universe Wallet?

Tari Universe Wallet is a reference-design mobile wallet app for the [Tari](https://www.tari.com/) digital currency. The goal is for creators and developers to use the open-source codebase as a starting point for their own Tari wallets and applications.

Want to contribute? Visit this repository for Android or [the iOS counterpart](https://github.com/tari-project/wallet-ios).

---

## Current State (as of v1.5.0)

The `development` branch is stable and production-ready. All features described below are fully implemented and merged.

### Exolix Crypto Swap Integration

A complete crypto swap feature has been added via the [Exolix](https://exolix.com) exchange API. Users can swap between Tari (XTM) and other cryptocurrencies directly from the wallet. The full flow is implemented:

| Screen | Description |
|--------|-------------|
| **Exchange** (`ExchangeFragment`) | Entry point: select direction (Buy/Sell XTM), enter amounts, pick rate type (fixed/floating). Rate auto-refreshes every 10 s. |
| **Select Currency** (`SelectCurrencyFragment`) | Paginated, searchable list of currencies and their networks fetched from Exolix API. |
| **Review** (`ExchangeReviewFragment`) | Confirms the swap details. For **Sell XTM**, sends funds via the Tari FFI wallet before creating the exchange. |
| **Send Funds** (`SendFundsFragment`) | Shows the deposit address and QR code the user must send external funds to (for **Buy XTM**). Creates the Exolix transaction on first load. |
| **Exchange Status** (`ExchangeStatusFragment`) | Polls the Exolix API every 10 s until the transaction reaches a terminal state (`success`, `refunded`, `overdue`). Includes a support email shortcut and a remove option. |
| **All Swaps** (`AllSwapsFragment`) | Lists all locally-stored pending swaps with refreshed statuses. Accessible from the Home screen bottom nav. |

Swap transaction IDs are persisted locally in `ExolixPrefRepository` (SharedPreferences), keyed per network.

**API key required** — see [Secrets & Configuration](#secrets--configuration).

### Other active features

- Tari FFI wallet v5.1.0 (libminotari_wallet_ffi)
- Onboarding, PIN/biometric auth, backup via seed phrase and Google Drive
- Send / Receive XTM transactions
- Contact book
- Airdrop integration
- RWA (Real World Assets) store
- Yat emoji ID support
- Sentry crash reporting
- Firebase Cloud Messaging (push notifications)
- Light / Dark / System theme

---

## Build Instructions

### Requirements

- **Android Studio** — latest stable version (the project uses AGP 8.11.0 and Kotlin 2.2.0; older Studio versions will not work)
- **NDK** and **CMake** — installed via Android Studio's SDK Manager (see step 4 below)
- **JDK 17** — Android Studio bundles one; no separate install needed
- **Supported ABIs**: `arm64-v8a` and `x86_64` only (no `armeabi-v7a` or `x86`)

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/tari-project/wallet-android.git
   ```

2. Open Android Studio → **Open an existing project** → select the project root.

3. Wait for Gradle sync to complete.

4. If NDK is not configured, Android Studio will show an "NDK not configured" message in the build log. Click **Install NDK** and accept the license. After installation, verify in **SDK Manager** that both **NDK** and **CMake** are installed.

5. **Set up secrets** (see [Secrets & Configuration](#secrets--configuration) below). The build will auto-generate `secret.properties` and `sentry.properties` from their `*-example.properties` counterparts, but they will contain placeholder values.

6. In **Build Variants**, select one of:
   - `esmeraldaDebug` — Esmeralda testnet, debug
   - `mainnetDebug` — Mainnet, debug
   - `esmeraldaRelease` / `mainnetRelease` — release variants (require signing config)

7. **Build → Make Project**.

### Command-line builds

```bash
# Debug builds
./gradlew assembleEsmeraldaDebug
./gradlew assembleMainnetDebug
```

### Emulator Configuration

Android emulators default to the `x86` ABI, which is **not supported**. When creating an AVD, select an **x86_64** system image under the **x86 Images** tab in the Virtual Device Configuration wizard.

---

## Secrets & Configuration

Several property files hold credentials. They are **never committed** to version control. The build script auto-generates them from `*-example.properties` on first build, but you must fill in real values for a working app.

### `secret.properties` (root directory)

Auto-generated from `secret-example.properties`.

| Key | Description | Required |
|-----|-------------|----------|
| `sentry.public_dsn` | Sentry project DSN for crash reporting | Optional |
| `service.notifications.api_key` | API key for Tari push-notification server | Optional |
| `exolix.api.key` | **Exolix API key** for the crypto swap feature. Register at [exolix.com/developers](https://exolix.com/developers) to obtain one. Without a valid key the exchange UI will appear but all API calls will fail. | **Required for Exchange** |

### `sentry.properties` (root directory)

Auto-generated from `sentry-example.properties`. Fill in if you want Sentry crash reporting (Gradle plugin uploads ProGuard mappings on release builds).

| Key | Description |
|-----|-------------|
| `defaults.project` | Sentry project slug |
| `defaults.url` | Sentry server URL |
| `defaults.org` | Sentry organisation slug |
| `auth.token` | Sentry auth token |

### `yat.properties` (root directory)

Auto-generated from `yat-example.properties`. Required for Yat emoji-ID integration.

| Key | Description |
|-----|-------------|
| `yat.name` | Yat organisation name |
| `yat.key` | Yat organisation key |
| `yat.returnUrl` | Return URL after Yat OAuth |

### `app/google-services.json`

Required for Firebase Cloud Messaging. Obtain from the Firebase Console for your Firebase project and place it at:

```
app/
└── google-services.json
```

Without this file the build will fail with a Firebase plugin error.

---

## Native Library (FFI)

The native Tari wallet library (`libminotari_wallet_ffi`) is automatically downloaded during the build from the [Tari GitHub releases](https://github.com/tari-project/tari/releases). The version is controlled by `TariBuildConfig.LibWallet.version` in `buildSrc/src/main/kotlin/TariBuildConfig.kt`.

The download task (`downloadLibwallet`) runs as part of `preBuild` and places binaries into `libwallet/`. **Any files you manually put there will be overwritten.**

To use a custom (locally-built) native library:
1. Comment out `preBuild.dependsOn("downloadLibwallet")` in `app/build.gradle.kts`.
2. Place your `.a` static library files in `libwallet/arm64-v8a/` and `libwallet/x86_64/`, and the header at `libwallet/wallet.h`.

For updating OpenSSL for Android: https://github.com/217heidai/openssl_for_android/releases

---

## Architecture Overview

The project follows a single-module MVVM architecture with Dagger 2 for dependency injection. Screens are implemented as Fragments (newer ones in Jetpack Compose, older ones in XML view binding), all hosted inside `CommonActivity` subclasses.

### Layer summary

```
app/src/main/java/com/tari/android/wallet/
│
├── application/          # App entry point, lifecycle, TariNavigator, deep links
│   └── walletManager/    # Owns the FFIWallet instance; manages wallet lifecycle
│
├── data/                 # All data sources
│   ├── exolix/           # Exolix API models and ExolixRepository
│   ├── sharedPrefs/      # SharedPrefs repositories (one per domain)
│   ├── airdrop/          # Airdrop API
│   ├── rwa/              # RWA store API
│   └── ...               # baseNode, contacts, tx, etc.
│
├── di/                   # Dagger 2: ApplicationComponent, ApplicationModule, RetrofitModule
│
├── ffi/                  # JNI/FFI wrappers around the Rust native library
│   ├── FFIWallet.kt      # Main wallet facade — all wallet operations
│   └── FFIBase.kt        # Base class managing native pointer lifecycle
│
├── infrastructure/       # Cross-cutting: backup, logging, permissions, security
│
├── model/                # Pure domain models (MicroTari, TariWalletAddress, Tx types…)
│
├── notification/         # FCM / push notification handling
│
├── ui/
│   ├── common/           # CommonFragment, CommonViewModel, CommonActivity base classes
│   ├── compose/          # TariDesignSystem, TariColors, TariTextStyles, TariShapes
│   ├── component/        # Reusable UI components
│   ├── dialog/           # Modular dialog system
│   └── screen/           # All screens, organised by feature
│       ├── home/         # HomeActivity (main shell) + HomeOverview + AllSwaps
│       ├── exchange/     # Exolix swap flow (Exchange → SelectCurrency → Review → SendFunds → Status)
│       ├── send/         # Send / Receive / Confirm flow
│       ├── onboarding/   # First-run onboarding
│       ├── settings/     # All settings screens (backup, network, theme, etc.)
│       ├── tx/           # Transaction history & details
│       └── ...
│
└── util/                 # Kotlin extensions, coroutine helpers, BroadcastEffectFlow
```

### Key architectural patterns

**Navigation** — All navigation is centralised in `TariNavigator` (singleton). Destinations are expressed as a sealed class `Navigation` (defined in `TariNavigator.kt`). From any ViewModel, call `tariNavigator.navigate(Navigation.SomeDestination(...))`. All screens are pushed onto a single Fragment back-stack on the current Activity.

**Dependency injection** — Dagger 2 with a single `ApplicationComponent`. Every ViewModel calls `component.inject(this)` in its `init` block to self-inject (the component is accessed via `DiContainer.appComponent`). `CommonViewModel` pre-injects the most-used singletons (`walletManager`, `tariNavigator`, `sharedPrefsRepository`, `dialogManager`, etc.).

**Wallet access** — Never access `FFIWallet` directly. Use `walletManager.doOnWalletRunning { wallet -> ... }` (suspending, waits until the wallet is started) or the `CommonViewModel.doOnWalletRunning { }` helper, which dispatches on the IO dispatcher.

**Compose design system** — All Compose screens must be wrapped in `TariDesignSystem(theme = ...) { ... }`. Access tokens via `TariDesignSystem.colors`, `TariDesignSystem.typography`, `TariDesignSystem.shapes`.

**Preference repositories** — Extend `CommonPrefRepository`. Keys are namespaced per active network via `TariNetwork.formatKey(key)`. Use `SharedPrefGsonDelegate` for serialised objects or `SharedPrefDelegate` for primitives.

**Exolix exchange flow** — The two directions have slightly different flows:
- **Buy XTM** (external → XTM): `Exchange → SendFunds` (creates transaction, shows deposit QR) → `ExchangeStatus`
- **Sell XTM** (XTM → external): `Exchange → Review` (sends XTM via FFI wallet) → `SendFunds` → `ExchangeStatus`
