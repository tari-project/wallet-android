package com.tari.android.wallet.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.YatAdapter.ConnectedWallet
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation.*
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.auth.AuthActivity
import com.tari.android.wallet.ui.screen.auth.FeatureAuthFragment
import com.tari.android.wallet.ui.screen.biometrics.ChangeBiometricsFragment
import com.tari.android.wallet.ui.screen.chat.addChat.AddChatFragment
import com.tari.android.wallet.ui.screen.chat.chatDetails.ChatDetailsFragment
import com.tari.android.wallet.ui.screen.contactBook.add.AddContactFragment
import com.tari.android.wallet.ui.screen.contactBook.add.SelectUserContactFragment
import com.tari.android.wallet.ui.screen.contactBook.details.ContactDetailsFragment
import com.tari.android.wallet.ui.screen.contactBook.link.ContactLinkFragment
import com.tari.android.wallet.ui.screen.contactBook.root.ContactBookFragment
import com.tari.android.wallet.ui.screen.debug.DebugNavigation
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity
import com.tari.android.wallet.ui.screen.home.HomeActivity
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewFragment
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.screen.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.screen.pinCode.EnterPinCodeFragment
import com.tari.android.wallet.ui.screen.profile.WalletInfoFragment
import com.tari.android.wallet.ui.screen.restore.activity.WalletRestoreActivity
import com.tari.android.wallet.ui.screen.restore.enterRestorationPassword.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.InputSeedWordsFragment
import com.tari.android.wallet.ui.screen.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.screen.send.addAmount.AddAmountFragment
import com.tari.android.wallet.ui.screen.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.ui.screen.send.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.ui.screen.send.requestTari.RequestTariFragment
import com.tari.android.wallet.ui.screen.send.transfer.TransferFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.about.TariAboutFragment
import com.tari.android.wallet.ui.screen.settings.backgroundService.BackgroundServiceSettingsFragment
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.BackupOnboardingFlowFragment
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.BackupSettingsFragment
import com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.screen.settings.backup.enterCurrentPassword.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase.VerifySeedPhraseFragment
import com.tari.android.wallet.ui.screen.settings.backup.writeDownSeedWords.WriteDownSeedPhraseFragment
import com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.screen.settings.bluetoothSettings.BluetoothSettingsFragment
import com.tari.android.wallet.ui.screen.settings.dataCollection.DataCollectionFragment
import com.tari.android.wallet.ui.screen.settings.deleteWallet.DeleteWalletFragment
import com.tari.android.wallet.ui.screen.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.ui.screen.settings.screenRecording.ScreenRecordingSettingsFragment
import com.tari.android.wallet.ui.screen.settings.themeSelector.ThemeSelectorFragment
import com.tari.android.wallet.ui.screen.settings.torBridges.TorBridgesSelectionFragment
import com.tari.android.wallet.ui.screen.settings.torBridges.customBridges.CustomTorBridgesFragment
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsFragment
import com.tari.android.wallet.ui.screen.tx.history.all.AllTxHistoryFragment
import com.tari.android.wallet.ui.screen.tx.history.contact.ContactTxHistoryFragment
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
    lateinit var currentActivity: CommonActivity<*, *>

    fun navigate(navigation: Navigation) {
        when (navigation) {
            is EnterPinCode -> addFragment(EnterPinCodeFragment.newInstance(navigation.behavior, navigation.stashedPin))
            is ChangeBiometrics -> addFragment(ChangeBiometricsFragment())
            is SplashScreen -> toSplashActivity(navigation.seedWords, navigation.clearTop)
            is Home -> toHomeActivity(navigation.uri)
            is BackToHome -> popUpTo(HomeOverviewFragment::class.java.simpleName)

            is Restore.WalletRestoreActivity -> currentActivity.startActivity(Intent(currentActivity, WalletRestoreActivity::class.java))
            is Restore.ToEnterRestorePassword -> addFragment(EnterRestorationPasswordFragment())
            is Restore.ToRestoreWithRecoveryPhrase -> addFragment(InputSeedWordsFragment())

            is Auth.AuthScreen -> toAuthActivity(navigation.uri)
            is Auth.FeatureAuth -> addFragment(FeatureAuthFragment())
            is Auth.BackAfterAuth -> backAfterAuth()

            is ContactBook.ToAddContact -> addFragment(AddContactFragment())
            is ContactBook.ToContactDetails -> addFragment(ContactDetailsFragment.createFragment(navigation.contact))
            is ContactBook.ToRequestTari -> sendToUser(navigation.contact)
            is ContactBook.ToSendTari -> sendToUser(navigation.contact)
            is ContactBook.ToLinkContact -> addFragment(ContactLinkFragment.createFragment(navigation.contact))
            is ContactBook.BackToContactBook -> popUpTo(ContactBookFragment::class.java.simpleName)
            is ContactBook.ToExternalWallet -> toExternalWallet(navigation.connectedWallet)
            is ContactBook.ToContactTransactionHistory -> addFragment(ContactTxHistoryFragment.createFragment(navigation.contact))
            is ContactBook.ToAddPhoneContact -> toAddPhoneContact()
            is ContactBook.ToSelectTariUser -> addFragment(SelectUserContactFragment.newInstance())

            is AllSettings.ToBugReporting -> DebugActivity.launch(currentActivity, DebugNavigation.BugReport)
            is AllSettings.ToMyProfile -> addFragment(WalletInfoFragment())
            is AllSettings.ToAbout -> addFragment(TariAboutFragment())
            is AllSettings.ToBackgroundService -> addFragment(BackgroundServiceSettingsFragment())
            is AllSettings.ToScreenRecording -> addFragment(ScreenRecordingSettingsFragment())
            is AllSettings.ToBluetoothSettings -> addFragment(BluetoothSettingsFragment())
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

            is AddAmount.ContinueToAddNote -> addFragment(AddNoteFragment.newInstance(navigation.transactionData))
            is AddAmount.ContinueToFinalizing -> continueToFinalizeSendTx(navigation.transactionData)

            is TxList.ToChat -> toChat()
            is TxList.ToTxDetails -> addFragment(TxDetailsFragment.newInstance(navigation.tx, navigation.txId))
            is TxList.ToSendTariToUser -> sendToUser(navigation.contact, navigation.amount, navigation.note)
            is TxList.ToUtxos -> addFragment(UtxosListFragment())
            is TxList.ToAllSettings -> toAllSettings()
            is TxList.ToTransfer -> addFragment(TransferFragment())
            is TxList.HomeTransactionHistory -> addFragment(AllTxHistoryFragment())

            is TorBridge.ToCustomBridges -> addFragment(CustomTorBridgesFragment())

            is VerifySeedPhrase.ToSeedPhraseVerification -> addFragment(VerifySeedPhraseFragment.newInstance(navigation.seedWords))

            is BackupSettings.ToChangePassword -> addFragment(ChangeSecurePasswordFragment())
            is BackupSettings.ToConfirmPassword -> addFragment(EnterCurrentPasswordFragment())
            is BackupSettings.ToWalletBackupWithRecoveryPhrase -> addFragment(WriteDownSeedPhraseFragment())
            is BackupSettings.ToLearnMore -> addFragment(BackupOnboardingFlowFragment())

            is CustomBridge.UploadQrCode -> Unit

            is Chat.ToChat -> toChatDetail(navigation.walletAddress, navigation.isNew)
            is Chat.ToAddChat -> addFragment(AddChatFragment())
        }
    }

    fun navigateSequence(vararg navigations: Navigation) {
        navigations.forEach { navigate(it) }
    }

    private fun addFragment(fragment: CommonXmlFragment<*, *>, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) {
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

    private fun sendToUser(recipientUser: ContactDto, amount: MicroTari? = null, note: String = "") {
        val contact = currentActivity.intent.parcelable<ContactDto>(PARAMETER_CONTACT) ?: recipientUser
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

    private fun toAddPhoneContact() {
        currentActivity.startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))
    }

    private fun toBaseNodeSelection() = addFragment(ChangeBaseNodeFragment())

    private fun continueToFinalizeSendTx(transactionData: TransactionData) {
        if (transactionData.recipientContact?.yat != null) {
            yatAdapter.showOutcomingFinalizeActivity(this.currentActivity, transactionData)
        } else {
            addFragment(FinalizeSendTxFragment.create(transactionData))
        }
    }

    private fun toChat() = (currentActivity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_CHAT, NO_SMOOTH_SCROLL)
    private fun toAllSettings() = (currentActivity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)

    private fun toExternalWallet(connectedWallet: ConnectedWallet) {
        try {
            val externalAddress = connectedWallet.getExternalLink()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalAddress))

            if (intent.resolveActivity(currentActivity.packageManager) != null) {
                currentActivity.startActivity(intent)
            } else {
                currentActivity.viewModel.showWalletErrorDialog()
            }
        } catch (e: Throwable) {
            currentActivity.viewModel.showWalletErrorDialog()
        }
    }

    companion object {
        const val PARAMETER_NOTE = "note"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_TRANSACTION = "transaction_data"
        const val PARAMETER_CONTACT = "tari_contact_dto_args"

        const val INDEX_HOME = 0
        const val INDEX_CONTACT_BOOK = 1
        const val INDEX_CHAT = 2
        const val INDEX_SETTINGS = 3
        const val NO_SMOOTH_SCROLL = false
    }
}