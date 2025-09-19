package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.Navigation.AllSettings
import com.tari.android.wallet.application.Navigation.ContactBook.AllContacts
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.pinCode.PinCodeScreenBehavior

abstract class CommonSettingsViewModel : CommonViewModel() {

    fun onSettingClick(setting: Setting) {
        when (setting) {
            Setting.Profile -> tariNavigator.navigate(AllSettings.MyProfile)
            Setting.Contacts -> tariNavigator.navigate(AllContacts())
            Setting.WalletSettings -> tariNavigator.navigate(AllSettings.CategoryWalletSettings)
            Setting.Support -> tariNavigator.navigate(AllSettings.CategorySupportSettings)
            Setting.Legal -> tariNavigator.navigate(AllSettings.CategoryLegalSettings)
            Setting.Backup -> runWithAuthorization { tariNavigator.navigate(AllSettings.BackupSettings(true)) }
            Setting.DataCollection -> tariNavigator.navigate(AllSettings.DataCollection)
            Setting.ChangePasscode -> runWithAuthorization { tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.ChangeNew)) }
            Setting.CreatePasscode -> runWithAuthorization { tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.Create)) }
            Setting.Biometrics -> runWithAuthorization { tariNavigator.navigate(Navigation.ChangeBiometrics) }
            Setting.TtlStore -> openUrl(resourceManager.getString(R.string.ttl_store_url))
            Setting.About -> tariNavigator.navigate(AllSettings.About)
            Setting.ReportBug -> tariNavigator.navigate(AllSettings.BugReporting)
            Setting.VisitTari -> openUrl(resourceManager.getString(R.string.tari_url))
            Setting.Contribute -> openUrl(resourceManager.getString(R.string.github_repo_url))
            Setting.UserAgreement -> openUrl(resourceManager.getString(R.string.user_agreement_url))
            Setting.PrivacyPolicy -> openUrl(resourceManager.getString(R.string.privacy_policy_url))
            Setting.Disclaimer -> openUrl(resourceManager.getString(R.string.disclaimer_url))
            Setting.BlockExplorer -> openUrl(networkRepository.currentNetwork.blockExplorerBaseUrl.orEmpty())
            Setting.SelectTheme -> tariNavigator.navigate(AllSettings.ThemeSelection)
            Setting.ScreenRecording -> tariNavigator.navigate(AllSettings.ScreenRecording)
            Setting.SelectNetwork -> tariNavigator.navigate(AllSettings.NetworkSelection)
            Setting.DeleteWallet -> tariNavigator.navigate(AllSettings.DeleteWallet)
        }
    }
}