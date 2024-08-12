package com.tari.android.wallet.ui.fragment.home.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.showInternetConnectionErrorDialog
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.auth.FeatureAuthFragment
import com.tari.android.wallet.ui.fragment.biometrics.ChangeBiometricsFragment
import com.tari.android.wallet.ui.fragment.chat.addChat.AddChatFragment
import com.tari.android.wallet.ui.fragment.chat.chatDetails.ChatDetailsFragment
import com.tari.android.wallet.ui.fragment.contactBook.add.AddContactFragment
import com.tari.android.wallet.ui.fragment.contactBook.add.SelectUserContactFragment
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contactBook.details.ContactDetailsFragment
import com.tari.android.wallet.ui.fragment.contactBook.link.ContactLinkFragment
import com.tari.android.wallet.ui.fragment.contactBook.root.ContactBookFragment
import com.tari.android.wallet.ui.fragment.tx.history.TransactionHistoryFragment
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.tx.history.HomeTransactionHistoryFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.AddAmountNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.AllSettingsNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.BackupSettingsNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.ChatNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.ChooseRestoreOptionNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.CustomBridgeNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.EnterRestorationPasswordNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.InputSeedWordsNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.TorBridgeNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.TxListNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.VerifySeedPhraseNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.WalletRestoringFromSeedWordsNavigation
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.pinCode.EnterPinCodeFragment
import com.tari.android.wallet.ui.fragment.profile.WalletInfoFragment
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.InputSeedWordsFragment
import com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords.WalletRestoringFromSeedWordsFragment
import com.tari.android.wallet.ui.fragment.send.addAmount.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.send.requestTari.RequestTariFragment
import com.tari.android.wallet.ui.fragment.send.transfer.TransferFragment
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.allSettings.about.TariAboutFragment
import com.tari.android.wallet.ui.fragment.settings.backgroundService.BackgroundServiceSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.BackupOnboardingFlowFragment
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.changeSecurePassword.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.enterCurrentPassword.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase.VerifySeedPhraseFragment
import com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.WriteDownSeedPhraseFragment
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.BluetoothSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.dataCollection.DataCollectionFragment
import com.tari.android.wallet.ui.fragment.settings.deleteWallet.DeleteWalletFragment
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugNavigation
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.ui.fragment.settings.screenRecording.ScreenRecordingSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.themeSelector.ThemeSelectorFragment
import com.tari.android.wallet.ui.fragment.settings.torBridges.TorBridgesSelectionFragment
import com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges.CustomTorBridgesFragment
import com.tari.android.wallet.ui.fragment.tx.HomeFragment
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment
import com.tari.android.wallet.ui.fragment.utxos.list.UtxosListFragment
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

// TODO: move navigation logic to only the navigate() method and make all navigation methods private
@Singleton
class TariNavigator @Inject constructor(val prefs: CorePrefRepository, val tariSettingsSharedRepository: TariSettingsPrefRepository) {

    lateinit var activity: CommonActivity<*, *>

    fun navigate(navigation: Navigation) {
        when (navigation) {
            is Navigation.EnterPinCodeNavigation -> addFragment(EnterPinCodeFragment.newInstance(navigation.behavior, navigation.stashedPin))
            is Navigation.ChangeBiometrics -> addFragment(ChangeBiometricsFragment())
            is Navigation.FeatureAuth -> addFragment(FeatureAuthFragment())
            is ContactBookNavigation.ToAddContact -> toAddContact()
            is ContactBookNavigation.ToContactDetails -> toContactDetails(navigation.contact)
            is ContactBookNavigation.ToRequestTari -> toRequestTariFromContact(navigation.contact)
            is ContactBookNavigation.ToSendTari -> toSendTariToContact(navigation.contact)
            is ContactBookNavigation.ToLinkContact -> toLinkContact(navigation.contact)
            is ContactBookNavigation.BackToContactBook -> backToContactBook()
            is ContactBookNavigation.ToExternalWallet -> toExternalWallet(navigation.connectedWallet)
            is ContactBookNavigation.ToContactTransactionHistory -> toContactTransactionHistory(navigation.contact)
            is ContactBookNavigation.ToAddPhoneContact -> toAddPhoneContact()
            is ContactBookNavigation.ToSelectTariUser -> addFragment(SelectUserContactFragment.newInstance())
            is ChooseRestoreOptionNavigation.ToEnterRestorePassword -> toEnterRestorePassword()
            is ChooseRestoreOptionNavigation.OnRestoreCompleted -> onRestoreCompleted()
            is ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase -> toRestoreWithRecoveryPhrase()
            is AllSettingsNavigation.ToBugReporting -> DebugActivity.launch(activity, DebugNavigation.BugReport)
            is AllSettingsNavigation.ToMyProfile -> toMyProfile()
            is AllSettingsNavigation.ToAbout -> toAbout()
            is AllSettingsNavigation.ToBackgroundService -> toBackgroundService()
            is AllSettingsNavigation.ToScreenRecording -> toScreenRecording()
            is AllSettingsNavigation.ToBluetoothSettings -> addFragment(BluetoothSettingsFragment())
            is AllSettingsNavigation.ToBackupSettings -> toBackupSettings(true)
            is AllSettingsNavigation.ToBaseNodeSelection -> toBaseNodeSelection()
            is AllSettingsNavigation.ToDeleteWallet -> toDeleteWallet()
            is AllSettingsNavigation.ToNetworkSelection -> toNetworkSelection()
            is AllSettingsNavigation.ToTorBridges -> toTorBridges()
            is AllSettingsNavigation.ToDataCollection -> addFragment(DataCollectionFragment())
            is AllSettingsNavigation.ToThemeSelection -> toThemeSelection()
            is AllSettingsNavigation.ToRequestTari -> addFragment(RequestTariFragment.newInstance())
            is EnterRestorationPasswordNavigation.OnRestore -> onRestoreCompleted()
            is InputSeedWordsNavigation.ToRestoreFormSeedWordsInProgress -> toRestoreFromSeedWordsInProgress()
            is InputSeedWordsNavigation.ToBaseNodeSelection -> toBaseNodeSelection()
            is WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted -> onRestoreCompleted()
            is WalletRestoringFromSeedWordsNavigation.OnRestoreFailed -> {
//                changeOnBackPressed(false)
                onBackPressed()
            }

            is AddAmountNavigation.OnAmountExceedsActualAvailableBalance -> onAmountExceedsActualAvailableBalance()
            is AddAmountNavigation.ContinueToAddNote -> continueToAddNote(navigation.transactionData)
            is AddAmountNavigation.ContinueToFinalizing -> continueToFinalizeSendTx(navigation.transactionData)
            is TxListNavigation.ToChat -> toChat()
            is TxListNavigation.ToTxDetails -> toTxDetails(navigation.tx, null)
            is TxListNavigation.ToSendTariToUser -> toSendTari(navigation.contact, navigation.amount)
            is TxListNavigation.ToSendWithDeeplink -> toSendWithDeeplink(navigation.sendDeeplink)
            is TxListNavigation.ToUtxos -> toUtxos()
            is TxListNavigation.ToAllSettings -> toAllSettings()
            is TxListNavigation.ToSplashScreen -> toSplash()
            is TxListNavigation.ToTransfer -> addFragment(TransferFragment())
            is TxListNavigation.HomeTransactionHistory -> addFragment(HomeTransactionHistoryFragment())
            is TorBridgeNavigation.ToCustomBridges -> toCustomTorBridges()
            is VerifySeedPhraseNavigation.ToSeedPhraseVerificationComplete -> onSeedPhraseVerificationComplete()
            is VerifySeedPhraseNavigation.ToSeedPhraseVerification -> toSeedPhraseVerification(navigation.seedWords)
            is BackupSettingsNavigation.ToChangePassword -> toChangePassword()
            is BackupSettingsNavigation.ToConfirmPassword -> toConfirmPassword()
            is BackupSettingsNavigation.ToWalletBackupWithRecoveryPhrase -> toWalletBackupWithRecoveryPhrase()
            is BackupSettingsNavigation.ToLearnMore -> toBackupOnboardingFlow()
            is CustomBridgeNavigation.UploadQrCode -> Unit
            is ChatNavigation.ToChat -> toChatDetail(navigation.walletAddress, navigation.isNew)
            is ChatNavigation.ToAddChat -> addFragment(AddChatFragment())
        }
    }

    private fun toSplash() {
        val intent = Intent(activity, OnboardingFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finishAffinity()
    }

    private fun toAddPhoneContact() {
        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
        activity.startActivity(intent)
    }

    private fun toEnterRestorePassword() = addFragment(EnterRestorationPasswordFragment.newInstance())

    private fun toRestoreWithRecoveryPhrase() = addFragment(InputSeedWordsFragment.newInstance())

    private fun toRestoreFromSeedWordsInProgress() = addFragment(WalletRestoringFromSeedWordsFragment.newInstance())

    private fun onRestoreCompleted() {
        // wallet restored, setup shared prefs accordingly
        prefs.onboardingCompleted = true
        prefs.onboardingStarted = true
        prefs.onboardingAuthSetupStarted = true
        prefs.onboardingAuthSetupCompleted = false
        prefs.onboardingDisplayedAtHome = true
        tariSettingsSharedRepository.isRestoredWallet = true

        activity.finish()
        activity.startActivity(Intent(this.activity, OnboardingFlowActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    fun onBackPressed() = activity.onBackPressed()

    fun toTxDetails(tx: Tx?, txId: TxId?) = activity.addFragment(TxDetailsFragment().apply {
        arguments = Bundle().apply {
            putParcelable(TxDetailsFragment.TX_EXTRA_KEY, tx)
            putParcelable(TxDetailsFragment.TX_ID_EXTRA_KEY, txId)
        }
    })

    private fun toChat() = (activity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_CHAT, NO_SMOOTH_SCROLL)

    fun toAllSettings() = (activity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)

    fun toBackupSettings(withAnimation: Boolean) = addFragment(BackupSettingsFragment(), withAnimation = withAnimation)

    private fun toDeleteWallet() = addFragment(DeleteWalletFragment())

    private fun toBackgroundService() = addFragment(BackgroundServiceSettingsFragment())

    private fun toScreenRecording() = addFragment(ScreenRecordingSettingsFragment())

    private fun toMyProfile() = addFragment(WalletInfoFragment())

    private fun toAbout() = addFragment(TariAboutFragment())

    private fun toBackupOnboardingFlow() = addFragment(BackupOnboardingFlowFragment())

    private fun toBaseNodeSelection() = addFragment(ChangeBaseNodeFragment())

    private fun toTorBridges() = addFragment(TorBridgesSelectionFragment())

    private fun toThemeSelection() = addFragment(ThemeSelectorFragment())

    private fun toUtxos() = addFragment(UtxosListFragment())

    private fun toCustomTorBridges() = addFragment(CustomTorBridgesFragment())

    private fun toNetworkSelection() = addFragment(NetworkSelectionFragment())

    fun toWalletBackupWithRecoveryPhrase() = addFragment(WriteDownSeedPhraseFragment())

    private fun toSeedPhraseVerification(seedWords: List<String>) = addFragment(VerifySeedPhraseFragment.newInstance(seedWords))

    private fun toConfirmPassword() = addFragment(EnterCurrentPasswordFragment())

    fun toChangePassword() = addFragment(ChangeSecurePasswordFragment())

    private fun toSendTari(user: ContactDto, amount: MicroTari?) = sendToUser(user, amount)

    private fun toSendWithDeeplink(deeplink: DeepLink.Send) {
        popUpTo(HomeFragment::class.java.simpleName)
        sendToUserByDeeplink(deeplink)
    }

    private fun toAddContact() = addFragment(AddContactFragment())

    private fun toContactDetails(contact: ContactDto) = addFragment(ContactDetailsFragment.createFragment(contact))

    private fun toRequestTariFromContact(contact: ContactDto) = sendToUser(contact)

    private fun toSendTariToContact(contact: ContactDto) = sendToUser(contact)

    private fun backToContactBook() = popUpTo(ContactBookFragment::class.java.simpleName)

    fun backToAllSettings() = popUpTo(AllSettingsFragment::class.java.simpleName)

    fun backAfterAuth() {
        if (activity is HomeActivity) {
            popUpTo(AllSettingsFragment::class.java.simpleName)
        } else {
            popUpTo(LocalAuthFragment::class.java.simpleName)
        }
    }

    private fun toLinkContact(contact: ContactDto) = addFragment(ContactLinkFragment.createFragment(contact))

    private fun toContactTransactionHistory(contact: ContactDto) = addFragment(TransactionHistoryFragment.createFragment(contact))

    private fun toExternalWallet(connectedWallet: YatDto.ConnectedWallet) {
        try {
            val externalAddress = connectedWallet.getExternalLink()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalAddress))

            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                activity.viewModel.openWalletErrorDialog()
            }
        } catch (e: Throwable) {
            activity.viewModel.openWalletErrorDialog()
        }
    }

    private fun onAmountExceedsActualAvailableBalance() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(activity.string(R.string.error_balance_exceeded_title)),
                BodyModule(activity.string(R.string.error_balance_exceeded_description)),
                ButtonModule(activity.string(R.string.common_close), ButtonStyle.Close),
            )
        )
        ModularDialog(activity, args).show()
    }

    private fun continueToAddNote(transactionData: TransactionData) {
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this.activity)
            return
        }
        val bundle = Bundle().apply {
            putParcelable(PARAMETER_TRANSACTION, transactionData)
            activity.intent.getStringExtra(PARAMETER_NOTE)?.let { putString(PARAMETER_NOTE, it) }
        }
        addFragment(AddNoteFragment(), bundle)
    }

    fun continueToFinalizeSendTx(transactionData: TransactionData) {
        if (transactionData.recipientContact?.yatDto != null) {
            (activity as HomeActivity).viewModel.yatAdapter.showOutcomingFinalizeActivity(this.activity, transactionData)
        } else {
            addFragment(FinalizeSendTxFragment.create(transactionData))
        }
    }

    fun onSendTxFailure(isYat: Boolean, txFailureReason: TxFailureReason) {
        EventBus.post(Event.Transaction.TxSendFailed(txFailureReason))
        navigateBackFromTxSend(isYat)
    }

    fun onSendTxSuccessful(isYat: Boolean, txId: TxId) {
        EventBus.post(Event.Transaction.TxSendSuccessful(txId))
        navigateBackFromTxSend(isYat)
    }

    private fun navigateBackFromTxSend(isYat: Boolean) {
        val fragmentsCount = activity.supportFragmentManager.fragments.size - 5
        for (i in 0 until fragmentsCount) {
            activity.supportFragmentManager.popBackStackImmediate()
        }
    }

    fun onPasswordChanged() {
        popUpTo(BackupSettingsFragment::class.java.simpleName)
    }

    private fun onSeedPhraseVerificationComplete() {
        popUpTo(BackupSettingsFragment::class.java.simpleName)
    }

    private fun sendToUserByDeeplink(deeplink: DeepLink.Send) {
        FFIWallet.instance?.getWalletAddress()
        val address = TariWalletAddress.fromBase58(deeplink.walletAddress)
        val contact = (activity as HomeActivity).viewModel.contactsRepository.getContactByAddress(address)
        val bundle = Bundle().apply {
            putParcelable(PARAMETER_CONTACT, contact)
            putParcelable(PARAMETER_AMOUNT, deeplink.amount)
        }

        addFragment(AddAmountFragment(), bundle)
    }

    private fun sendToUser(recipientUser: ContactDto, amount: MicroTari? = null) {
        val bundle = Bundle().apply {
            putParcelable(PARAMETER_CONTACT, recipientUser)
            val innerAmount = (activity.intent.getDoubleExtra(PARAMETER_AMOUNT, Double.MIN_VALUE))
            val tariAmount = amount ?: if (innerAmount != Double.MIN_VALUE) MicroTari(BigInteger.valueOf(innerAmount.toLong())) else null
            tariAmount?.let { putParcelable(PARAMETER_AMOUNT, it) }
            activity.intent.parcelable<ContactDto>(PARAMETER_CONTACT)?.let { putParcelable(PARAMETER_CONTACT, it) }
        }

        addFragment(AddAmountFragment(), bundle)
    }

    private fun toChatDetail(walletAddress: TariWalletAddress, isNew: Boolean) {
        if (isNew) {
            onBackPressed()
        }

        addFragment(ChatDetailsFragment.newInstance(walletAddress))
    }

    private fun addFragment(fragment: CommonFragment<*, *>, bundle: Bundle? = null, isRoot: Boolean = false, withAnimation: Boolean = true) =
        activity.addFragment(fragment, bundle, isRoot, withAnimation)

    //popup fragment
    private fun popUpTo(tag: String) = activity.popUpTo(tag)

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