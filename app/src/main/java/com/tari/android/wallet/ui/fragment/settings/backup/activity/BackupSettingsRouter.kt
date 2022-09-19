package com.tari.android.wallet.ui.fragment.settings.backup.activity

import androidx.fragment.app.Fragment

interface BackupSettingsRouter {
    fun toWalletBackupWithRecoveryPhrase(sourceFragment: Fragment)

    fun toSeedPhraseVerification(sourceFragment: Fragment, seedWords: List<String>)

    fun toConfirmPassword(sourceFragment: Fragment)

    fun toChangePassword(sourceFragment: Fragment)

    fun onPasswordChanged(sourceFragment: Fragment)

    fun onSeedPhraseVerificationComplete(sourceFragment: Fragment)
}