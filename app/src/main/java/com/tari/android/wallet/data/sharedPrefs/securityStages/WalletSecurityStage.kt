package com.tari.android.wallet.data.sharedPrefs.securityStages

enum class WalletSecurityStage {
    // Seed Phrase Validated
    Stage1A,
    // Cloud Backup Enabled
    Stage1B,
    // Cloud Backup Encrypted
    Stage2,
    ///Tokens Moved to Cold Wallet
    Stage3
}