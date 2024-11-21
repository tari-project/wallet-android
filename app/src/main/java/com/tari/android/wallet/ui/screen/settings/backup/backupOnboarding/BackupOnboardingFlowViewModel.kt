package com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding

import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel

class BackupOnboardingFlowViewModel : CommonViewModel() {
    init {
        component.inject(this)
    }

    fun navigateToChangePassword() {
        tariNavigator.navigate(Navigation.BackupSettings.ToChangePassword)
    }

    fun navigateToWalletBackup() {
        tariNavigator.navigate(Navigation.BackupSettings.ToWalletBackupWithRecoveryPhrase)
    }
}