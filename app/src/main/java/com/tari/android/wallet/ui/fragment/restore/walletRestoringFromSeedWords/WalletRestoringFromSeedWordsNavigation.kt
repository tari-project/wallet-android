package com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords

sealed class WalletRestoringFromSeedWordsNavigation {
    object OnRestoreCompleted : WalletRestoringFromSeedWordsNavigation()
    object OnRestoreFailed : WalletRestoringFromSeedWordsNavigation()
}