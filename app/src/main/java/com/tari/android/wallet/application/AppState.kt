package com.tari.android.wallet.application

enum class AppState {
    Initializing,       // App is initializing, UIs and other services should not resume yet
    Ready,              // App is ready to be used
    Failed              // App initialized failed, it's not usable. User should be advised to contact support.
}