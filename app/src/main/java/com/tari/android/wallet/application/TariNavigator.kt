package com.tari.android.wallet.application

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.tari.android.wallet.application.Navigation.AllSettings
import com.tari.android.wallet.application.Navigation.Auth
import com.tari.android.wallet.application.Navigation.Back
import com.tari.android.wallet.application.Navigation.BackToHome
import com.tari.android.wallet.application.Navigation.BackupSettings
import com.tari.android.wallet.application.Navigation.ChangeBiometrics
import com.tari.android.wallet.application.Navigation.ContactBook
import com.tari.android.wallet.application.Navigation.CustomBridge
import com.tari.android.wallet.application.Navigation.EnterPinCode
import com.tari.android.wallet.application.Navigation.Home
import com.tari.android.wallet.application.Navigation.InputSeedWords
import com.tari.android.wallet.application.Navigation.Restore
import com.tari.android.wallet.application.Navigation.ShareText
import com.tari.android.wallet.application.Navigation.SplashScreen
import com.tari.android.wallet.application.Navigation.TorBridge
import com.tari.android.wallet.application.Navigation.TxList
import com.tari.android.wallet.application.Navigation.TxSend
import com.tari.android.wallet.application.Navigation.VerifySeedPhrase
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.screen.auth.AuthActivity
import com.tari.android.wallet.ui.screen.auth.FeatureAuthFragment
import com.tari.android.wallet.ui.screen.biometrics.ChangeBiometricsFragment
import com.tari.android.wallet.ui.screen.contactBook.add.AddContactFragment
import com.tari.android.wallet.ui.screen.contactBook.details.ContactDetailsFragment
import com.tari.android.wallet.ui.screen.contactBook.list.ContactListFragment
import com.tari.android.wallet.ui.screen.debug.DebugNavigation
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity
import com.tari.android.wallet.ui.screen.home.HomeActivity
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewFragment
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.screen.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.screen.pinCode.EnterPinCodeFragment
import com.tari.android.wallet.ui.screen.pinCode.PinCodeScreenBehavior
import com.tari.android.wallet.ui.screen.profile.walletInfo.WalletInfoFragment
import com.tari.android.wallet.ui.screen.restore.activity.WalletRestoreActivity
import com.tari.android.wallet.ui.screen.restore.enterRestorationPassword.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.InputSeedWordsFragment
import com.tari.android.wallet.ui.screen.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.screen.send.confirm.ConfirmFragment
import com.tari.android.wallet.ui.screen.send.obsolete.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.ui.screen.send.obsolete.requestTari.RequestTariFragment
import com.tari.android.wallet.ui.screen.send.receive.ReceiveFragment
import com.tari.android.wallet.ui.screen.send.send.SendFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.about.TariAboutFragment
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.BackupSettingsFragment
import com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.screen.settings.backup.enterCurrentPassword.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.BackupLearnMoreFragment
import com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase.VerifySeedPhraseFragment
import com.tari.android.wallet.ui.screen.settings.backup.writeDownSeedWords.WriteDownSeedPhraseFragment
import com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.screen.settings.dataCollection.DataCollectionFragment
import com.tari.android.wallet.ui.screen.settings.deleteWallet.DeleteWalletFragment
import com.tari.android.wallet.ui.screen.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.ui.screen.settings.screenRecording.ScreenRecordingSettingsFragment
import com.tari.android.wallet.ui.screen.settings.themeSelector.ThemeSelectorFragment
import com.tari.android.wallet.ui.screen.settings.torBridges.TorBridgesSelectionFragment
import com.tari.android.wallet.ui.screen.settings.torBridges.customBridges.CustomTorBridgesFragment
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsFragment
import com.tari.android.wallet.ui.screen.tx.history.TxHistoryFragment
import com.tari.android.wallet.ui.screen.utxos.list.UtxosListFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariNavigator @Inject constructor(
    private val yatAdapter: YatAdapter,
) {
    // The activity on which the navigation intents are performed.
    // Set in the #onResume method of the activity!
    lateinit var currentActivity: CommonActivity<*>

    fun navigate(navigation: Navigation) {
        hideSoftKeyboard()

        when (navigation) {
            is Back -> navigateBack()

            is EnterPinCode -> addFragment(EnterPinCodeFragment.newInstance(navigation.behavior, navigation.stashedPin))
            is ChangeBiometrics -> addFragment(ChangeBiometricsFragment())
            is SplashScreen -> toSplashActivity(navigation.seedWords, navigation.clearTop)
            is Home -> toHomeActivity(navigation.uri)
            is BackToHome -> popUpTo(HomeOverviewFragment::class.java.simpleName)

            is ShareText -> shareText(navigation.text)

            is Restore.WalletRestoreActivity -> currentActivity.startActivity(Intent(currentActivity, WalletRestoreActivity::class.java))
            is Restore.ToEnterRestorePassword -> addFragment(EnterRestorationPasswordFragment())
            is Restore.ToRestoreWithRecoveryPhrase -> addFragment(InputSeedWordsFragment())

            is Auth.AuthScreen -> toAuthActivity(navigation.uri)
            is Auth.FeatureAuth -> addFragment(FeatureAuthFragment())
            is Auth.BackAfterAuth -> backAfterAuth()

            is ContactBook.AllContacts -> addFragment(ContactListFragment.newInstance(navigation.startForSelectResult))
            is ContactBook.ContactDetails -> addFragment(ContactDetailsFragment.createFragment(navigation.contact))
            is ContactBook.AddContact -> addFragment(AddContactFragment())

            is AllSettings.ToBugReporting -> DebugActivity.launch(currentActivity, DebugNavigation.BugReport)
            is AllSettings.ToMyProfile -> addFragment(WalletInfoFragment())
            is AllSettings.ToAbout -> addFragment(TariAboutFragment())
            is AllSettings.ToScreenRecording -> addFragment(ScreenRecordingSettingsFragment())
            is AllSettings.BackToBackupSettings -> popUpTo(BackupSettingsFragment::class.java.simpleName)
            is AllSettings.ToBackupSettings -> addFragment(BackupSettingsFragment.newInstance(), withAnimation = navigation.withAnimation)
            is AllSettings.ToBaseNodeSelection -> toBaseNodeSelection()
            is AllSettings.ToDeleteWallet -> addFragment(DeleteWalletFragment())
            is AllSettings.ToNetworkSelection -> addFragment(NetworkSelectionFragment())
            is AllSettings.ToTorBridges -> addFragment(TorBridgesSelectionFragment())
            is AllSettings.ToDataCollection -> addFragment(DataCollectionFragment())
            is AllSettings.ToThemeSelection -> addFragment(ThemeSelectorFragment())
            is AllSettings.ToRequestTari -> addFragment(RequestTariFragment.newInstance())

            is InputSeedWords.ToRestoreFromSeeds -> addFragment(WalletRestoringFragment.newInstance())
            is InputSeedWords.ToBaseNodeSelection -> toBaseNodeSelection()

            is TxSend.ToFinalizing -> continueToFinalizeSendTx(navigation.transactionData)
            is TxSend.Send -> addFragment(SendFragment.newInstance(navigation.contact, navigation.amount, navigation.note))
            is TxSend.Confirm -> addFragment(ConfirmFragment.newInstance(navigation.transactionData))

            is TxList.ToTxDetails -> addFragment(TxDetailsFragment.newInstance(navigation.tx, navigation.showCloseButton))
            is TxList.ToUtxos -> addFragment(UtxosListFragment())
            is TxList.ToAllSettings -> addFragment(AllSettingsFragment.newInstance())
            is TxList.ToReceive -> addFragment(ReceiveFragment())
            is TxList.HomeTransactionHistory -> addFragment(TxHistoryFragment.newInstance())

            is TorBridge.ToCustomBridges -> addFragment(CustomTorBridgesFragment())

            is VerifySeedPhrase.ToSeedPhraseVerification -> addFragment(VerifySeedPhraseFragment.newInstance(navigation.seedWords))

            is BackupSettings.ToChangePassword -> addFragment(ChangeSecurePasswordFragment())
            is BackupSettings.ToConfirmPassword -> addFragment(EnterCurrentPasswordFragment())
            is BackupSettings.ToWalletBackupWithRecoveryPhrase -> addFragment(WriteDownSeedPhraseFragment())
            is BackupSettings.ToLearnMore -> addFragment(BackupLearnMoreFragment())

            is CustomBridge.UploadQrCode -> Unit
        }
    }

    fun navigateSequence(vararg navigations: Navigation) {
        navigations.forEach { navigate(it) }
    }

    fun navigateBack() {
        currentActivity.supportFragmentManager.popBackStack()
    }

    private fun shareText(text: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        currentActivity.startActivity(shareIntent)
    }

    private fun addFragment(fragment: CommonFragment<*>, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) {
        currentActivity.addFragment(fragment, bundle, isRoot, withAnimation)
    }

    //popup fragment
    private fun popUpTo(tag: String) = currentActivity.popUpTo(tag)

    private fun toSplashActivity(seedWords: List<String>? = null, clearTop: Boolean = true, uri: Uri? = null) {
        currentActivity.startActivity(Intent(currentActivity, OnboardingFlowActivity::class.java).apply {
            flags = if (clearTop) Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            else Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(OnboardingFlowActivity.ARG_SEED_WORDS, seedWords?.toTypedArray())
            uri?.let { setData(it) }
        })
        currentActivity.finishAffinity()
    }

    private fun toHomeActivity(uri: Uri? = null) {
        currentActivity.startActivity(Intent(currentActivity, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            uri?.let { setData(it) }
        })
    }

    private fun toAuthActivity(uri: Uri? = null) {
        currentActivity.startActivity(Intent(currentActivity, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            uri?.let { setData(it) }
        })
    }

    private fun backAfterAuth() {
        if (currentActivity is HomeActivity) {
            popUpTo(AllSettingsFragment::class.java.simpleName)
        } else {
            popUpTo(LocalAuthFragment::class.java.simpleName)
        }
    }

    private fun toBaseNodeSelection() = addFragment(ChangeBaseNodeFragment())

    private fun continueToFinalizeSendTx(transactionData: TransactionData) {
        if (transactionData.yat != null) {
            yatAdapter.showOutcomingFinalizeActivity(this.currentActivity, transactionData)
        } else {
            addFragment(FinalizeSendTxFragment.create(transactionData))
        }
    }

    private fun hideSoftKeyboard() {
        val imm = currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentActivity.currentFocus ?: View(currentActivity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

sealed class Navigation {

    data object Back : Navigation()

    data class EnterPinCode(val behavior: PinCodeScreenBehavior, val stashedPin: String? = null) : Navigation()
    data object ChangeBiometrics : Navigation()
    data class SplashScreen(val seedWords: List<String>? = null, val clearTop: Boolean = true) : Navigation()
    data class Home(val uri: Uri? = null) : Navigation()
    data object BackToHome : Navigation()

    data class ShareText(val text: String) : Navigation()

    sealed class Auth : Navigation() {
        data class AuthScreen(val uri: Uri? = null) : Auth()
        data object FeatureAuth : Auth()
        data object BackAfterAuth : Auth()
    }

    sealed class Restore : Navigation() {
        data object WalletRestoreActivity : Restore()
        data object ToEnterRestorePassword : Restore()
        data object ToRestoreWithRecoveryPhrase : Restore()
    }

    sealed class CustomBridge : Navigation() {
        data object UploadQrCode : CustomBridge()
    }

    sealed class BackupSettings : Navigation() {
        data object ToLearnMore : BackupSettings()
        data object ToWalletBackupWithRecoveryPhrase : BackupSettings()
        data object ToChangePassword : BackupSettings()
        data object ToConfirmPassword : BackupSettings()
    }

    sealed class VerifySeedPhrase : Navigation() {
        data class ToSeedPhraseVerification(val seedWords: List<String>) : VerifySeedPhrase()
    }

    sealed class TorBridge : Navigation() {
        data object ToCustomBridges : TorBridge()
    }

    sealed class TxList : Navigation() {
        data class ToTxDetails(val tx: Tx, val showCloseButton: Boolean = false) : TxList()
        data object ToAllSettings : TxList()
        data object ToUtxos : TxList()
        data object HomeTransactionHistory : TxList()
        data object ToReceive : TxList()
    }

    sealed class TxSend : Navigation() {
        data class ToFinalizing(val transactionData: TransactionData) : TxSend()
        data class Send(val contact: Contact? = null, val amount: MicroTari? = null, val note: String? = null) : TxSend()
        data class Confirm(val transactionData: TransactionData) : TxSend()
    }

    sealed class AllSettings : Navigation() {
        data object ToMyProfile : AllSettings()
        data object ToBugReporting : AllSettings()
        data object ToDataCollection : AllSettings()
        data object ToAbout : AllSettings()
        data object BackToBackupSettings : AllSettings()
        data class ToBackupSettings(val withAnimation: Boolean) : AllSettings()
        data object ToDeleteWallet : AllSettings()
        data object ToScreenRecording : AllSettings()
        data object ToThemeSelection : AllSettings()
        data object ToTorBridges : AllSettings()
        data object ToNetworkSelection : AllSettings()
        data object ToBaseNodeSelection : AllSettings()
        data object ToRequestTari : AllSettings()
    }

    sealed class InputSeedWords : Navigation() {
        data object ToRestoreFromSeeds : InputSeedWords()
        data object ToBaseNodeSelection : InputSeedWords()
    }

    sealed class ContactBook : Navigation() {
        data class AllContacts(val startForSelectResult: Boolean = false) : ContactBook()
        data class ContactDetails(val contact: Contact) : ContactBook()
        data object AddContact : ContactBook()
    }
}
