package com.tari.android.wallet.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation.AllSettings
import com.tari.android.wallet.navigation.Navigation.Auth
import com.tari.android.wallet.navigation.Navigation.BackToHome
import com.tari.android.wallet.navigation.Navigation.BackupSettings
import com.tari.android.wallet.navigation.Navigation.ChangeBiometrics
import com.tari.android.wallet.navigation.Navigation.Chat
import com.tari.android.wallet.navigation.Navigation.ContactBook
import com.tari.android.wallet.navigation.Navigation.CustomBridge
import com.tari.android.wallet.navigation.Navigation.EnterPinCode
import com.tari.android.wallet.navigation.Navigation.Home
import com.tari.android.wallet.navigation.Navigation.InputSeedWords
import com.tari.android.wallet.navigation.Navigation.Restore
import com.tari.android.wallet.navigation.Navigation.ShareText
import com.tari.android.wallet.navigation.Navigation.SplashScreen
import com.tari.android.wallet.navigation.Navigation.TorBridge
import com.tari.android.wallet.navigation.Navigation.TxList
import com.tari.android.wallet.navigation.Navigation.TxSend
import com.tari.android.wallet.navigation.Navigation.VerifySeedPhrase
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.screen.auth.AuthActivity
import com.tari.android.wallet.ui.screen.auth.FeatureAuthFragment
import com.tari.android.wallet.ui.screen.biometrics.ChangeBiometricsFragment
import com.tari.android.wallet.ui.screen.chat.addChat.AddChatFragment
import com.tari.android.wallet.ui.screen.chat.chatDetails.ChatDetailsFragment
import com.tari.android.wallet.ui.screen.contactBook.details.ContactDetailsFragment
import com.tari.android.wallet.ui.screen.contactBook.list.ContactListFragment
import com.tari.android.wallet.ui.screen.contactBook.obsolete.add.AddContactFragment
import com.tari.android.wallet.ui.screen.contactBook.obsolete.add.SelectUserContactFragment
import com.tari.android.wallet.ui.screen.contactBook.obsolete.root.ContactBookFragment
import com.tari.android.wallet.ui.screen.debug.DebugNavigation
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity
import com.tari.android.wallet.ui.screen.home.HomeActivity
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewFragment
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.screen.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.screen.pinCode.EnterPinCodeFragment
import com.tari.android.wallet.ui.screen.profile.walletInfo.WalletInfoFragment
import com.tari.android.wallet.ui.screen.restore.activity.WalletRestoreActivity
import com.tari.android.wallet.ui.screen.restore.enterRestorationPassword.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.InputSeedWordsFragment
import com.tari.android.wallet.ui.screen.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.screen.send.addAmount.AddAmountFragment
import com.tari.android.wallet.ui.screen.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.ui.screen.send.confirm.ConfirmFragment
import com.tari.android.wallet.ui.screen.send.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.ui.screen.send.receive.ReceiveFragment
import com.tari.android.wallet.ui.screen.send.requestTari.RequestTariFragment
import com.tari.android.wallet.ui.screen.send.transfer.TransferFragment
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
import com.tari.android.wallet.util.extension.parcelable
import java.math.BigInteger
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
        when (navigation) {
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

            is ContactBook.AllContacts -> addFragment(ContactListFragment())
            is ContactBook.ContactDetails -> addFragment(ContactDetailsFragment.createFragment(navigation.contact))
            is ContactBook.ToAddContact -> addFragment(AddContactFragment())
            is ContactBook.ToSendTari -> sendToUser(navigation.contact)
            is ContactBook.BackToContactBook -> popUpTo(ContactBookFragment::class.java.simpleName)
            is ContactBook.ToSelectTariUser -> addFragment(SelectUserContactFragment.newInstance())

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

            is TxSend.ToAddNote -> addFragment(AddNoteFragment.newInstance(navigation.transactionData))
            is TxSend.ToFinalizing -> continueToFinalizeSendTx(navigation.transactionData)
            is TxSend.ToConfirm -> addFragment(ConfirmFragment.newInstance(navigation.transactionData))

            is TxList.ToTxDetails -> addFragment(TxDetailsFragment.newInstance(navigation.tx, navigation.showCloseButton))
            is TxList.ToSendTariToUser -> sendToUser(navigation.contact, navigation.amount, navigation.note)
            is TxList.ToUtxos -> addFragment(UtxosListFragment())
            is TxList.ToAllSettings -> addFragment(AllSettingsFragment.newInstance())
            is TxList.ToTransfer -> addFragment(TransferFragment())
            is TxList.ToReceive -> addFragment(ReceiveFragment())
            is TxList.HomeTransactionHistory -> addFragment(TxHistoryFragment.newInstance())

            is TorBridge.ToCustomBridges -> addFragment(CustomTorBridgesFragment())

            is VerifySeedPhrase.ToSeedPhraseVerification -> addFragment(VerifySeedPhraseFragment.newInstance(navigation.seedWords))

            is BackupSettings.ToChangePassword -> addFragment(ChangeSecurePasswordFragment())
            is BackupSettings.ToConfirmPassword -> addFragment(EnterCurrentPasswordFragment())
            is BackupSettings.ToWalletBackupWithRecoveryPhrase -> addFragment(WriteDownSeedPhraseFragment())
            is BackupSettings.ToLearnMore -> addFragment(BackupLearnMoreFragment())

            is CustomBridge.UploadQrCode -> Unit

            is Chat.ToChat -> toChatDetail(navigation.walletAddress, navigation.isNew)
            is Chat.ToAddChat -> addFragment(AddChatFragment())
        }
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

    fun navigateSequence(vararg navigations: Navigation) {
        navigations.forEach { navigate(it) }
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

    private fun sendToUser(recipientUser: Contact, amount: MicroTari? = null, note: String = "") {
        val contact = currentActivity.intent.parcelable<Contact>(PARAMETER_CONTACT) ?: recipientUser
        val innerAmount = currentActivity.intent.getDoubleExtra(PARAMETER_AMOUNT, Double.MIN_VALUE)
        val tariAmount = amount ?: MicroTari(BigInteger.valueOf(innerAmount.toLong())).takeIf { innerAmount != Double.MIN_VALUE }

        addFragment(AddAmountFragment.newInstance(contact, tariAmount, note))
    }

    private fun toChatDetail(walletAddress: TariWalletAddress, isNew: Boolean) {
        if (isNew) {
            currentActivity.onBackPressed()
        }
        addFragment(ChatDetailsFragment.newInstance(walletAddress))
    }

    private fun toBaseNodeSelection() = addFragment(ChangeBaseNodeFragment())

    private fun continueToFinalizeSendTx(transactionData: TransactionData) {
        if (transactionData.yat != null) {
            yatAdapter.showOutcomingFinalizeActivity(this.currentActivity, transactionData)
        } else {
            addFragment(FinalizeSendTxFragment.create(transactionData))
        }
    }

    companion object {
        const val PARAMETER_NOTE = "note"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_TRANSACTION = "transaction_data"
        const val PARAMETER_CONTACT = "tari_contact_dto_args"
    }
}