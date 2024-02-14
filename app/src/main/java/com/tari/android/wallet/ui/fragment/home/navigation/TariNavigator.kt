package com.tari.android.wallet.ui.fragment.home.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.serializable
import com.tari.android.wallet.ui.extension.showInternetConnectionErrorDialog
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.auth.FeatureAuthFragment
import com.tari.android.wallet.ui.fragment.biometrics.ChangeBiometricsFragment
import com.tari.android.wallet.ui.fragment.chat_list.addChat.AddChatFragment
import com.tari.android.wallet.ui.fragment.chat_list.chat.ChatFragment
import com.tari.android.wallet.ui.fragment.contact_book.add.AddContactFragment
import com.tari.android.wallet.ui.fragment.contact_book.add.SelectUserContactFragment
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contact_book.details.ContactDetailsFragment
import com.tari.android.wallet.ui.fragment.contact_book.link.ContactLinkFragment
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookFragment
import com.tari.android.wallet.ui.fragment.contact_book.transactionHistory.TransactionHistoryFragment
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.home.homeTransactionHistory.HomeTransactionHistoryFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.AllSettingsNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.ContactBookNavigation
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
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.addBaseNode.AddCustomBaseNodeFragment
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.BluetoothSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.dataCollection.DataCollectionFragment
import com.tari.android.wallet.ui.fragment.settings.deleteWallet.DeleteWalletFragment
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugNavigation
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.ui.fragment.settings.themeSelector.ThemeSelectorFragment
import com.tari.android.wallet.ui.fragment.settings.torBridges.TorBridgesSelectionFragment
import com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges.CustomTorBridgesFragment
import com.tari.android.wallet.ui.fragment.tx.HomeFragment
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment
import com.tari.android.wallet.ui.fragment.utxos.list.UtxosListFragment
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TariNavigator @Inject constructor(val prefs: SharedPrefsRepository, val tariSettingsSharedRepository: TariSettingsSharedRepository) {

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
            Navigation.ChooseRestoreOptionNavigation.ToEnterRestorePassword -> toEnterRestorePassword()
            Navigation.ChooseRestoreOptionNavigation.OnRestoreCompleted -> onRestoreCompleted()
            Navigation.ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase -> toRestoreWithRecoveryPhrase()
            AllSettingsNavigation.ToBugReporting -> DebugActivity.launch(activity, DebugNavigation.BugReport)
            AllSettingsNavigation.ToMyProfile -> toMyProfile()
            AllSettingsNavigation.ToAbout -> toAbout()
            AllSettingsNavigation.ToBackgroundService -> toBackgroundService()
            AllSettingsNavigation.ToBluetoothSettings -> addFragment(BluetoothSettingsFragment())
            AllSettingsNavigation.ToBackupSettings -> toBackupSettings(true)
            AllSettingsNavigation.ToBaseNodeSelection -> toBaseNodeSelection()
            AllSettingsNavigation.ToDeleteWallet -> toDeleteWallet()
            AllSettingsNavigation.ToNetworkSelection -> toNetworkSelection()
            AllSettingsNavigation.ToTorBridges -> toTorBridges()
            AllSettingsNavigation.ToDataCollection -> addFragment(DataCollectionFragment())
            AllSettingsNavigation.ToThemeSelection -> toThemeSelection()
            AllSettingsNavigation.ToRequestTari -> addFragment(RequestTariFragment.newInstance())
            Navigation.EnterRestorationPasswordNavigation.OnRestore -> onRestoreCompleted()
            Navigation.InputSeedWordsNavigation.ToRestoreFormSeedWordsInProgress -> toRestoreFromSeedWordsInProgress()
            Navigation.InputSeedWordsNavigation.ToBaseNodeSelection -> toBaseNodeSelection()
            Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted -> onRestoreCompleted()
            Navigation.WalletRestoringFromSeedWordsNavigation.OnRestoreFailed -> {
//                changeOnBackPressed(false)
                onBackPressed()
            }

            Navigation.AddAmountNavigation.OnAmountExceedsActualAvailableBalance -> onAmountExceedsActualAvailableBalance()
            is Navigation.AddAmountNavigation.ContinueToAddNote -> continueToAddNote(navigation.transactionData)
            is Navigation.AddAmountNavigation.ContinueToFinalizing -> continueToFinalizeSendTx(navigation.transactionData)
            Navigation.TxListNavigation.ToChat -> toChat()
            is Navigation.TxListNavigation.ToTxDetails -> toTxDetails(navigation.tx, null)
            is Navigation.TxListNavigation.ToSendTariToUser -> toSendTari(navigation.contact, navigation.amount)
            is Navigation.TxListNavigation.ToSendWithDeeplink -> toSendWithDeeplink(navigation.sendDeeplink)
            Navigation.TxListNavigation.ToUtxos -> toUtxos()
            Navigation.TxListNavigation.ToAllSettings -> toAllSettings()
            Navigation.TxListNavigation.ToSplashScreen -> toSplash()
            is Navigation.TxListNavigation.ToTransfer -> addFragment(TransferFragment())
            is Navigation.TxListNavigation.HomeTransactionHistory -> addFragment(HomeTransactionHistoryFragment())
            Navigation.TorBridgeNavigation.ToCustomBridges -> toCustomTorBridges()
            Navigation.BaseNodeNavigation.ToAddCustomBaseNode -> toAddCustomBaseNode()
            Navigation.VerifySeedPhraseNavigation.ToSeedPhraseVerificationComplete -> onSeedPhraseVerificationComplete()
            is Navigation.VerifySeedPhraseNavigation.ToSeedPhraseVerification -> toSeedPhraseVerification(navigation.seedWords)
            Navigation.BackupSettingsNavigation.ToChangePassword -> toChangePassword()
            Navigation.BackupSettingsNavigation.ToConfirmPassword -> toConfirmPassword()
            Navigation.BackupSettingsNavigation.ToWalletBackupWithRecoveryPhrase -> toWalletBackupWithRecoveryPhrase()
            Navigation.BackupSettingsNavigation.ToLearnMore -> toBackupOnboardingFlow()
            Navigation.CustomBridgeNavigation.UploadQrCode -> Unit
            is Navigation.ChatNavigation.ToChat -> {
                if (navigation.isNew) {
                    onBackPressed()
                }

                addFragment(ChatFragment.newInstance(navigation.walletAddress))
            }

            Navigation.ChatNavigation.ToAddChat -> addFragment(AddChatFragment())
            else -> Unit
        }
    }

    private fun toSplash() {
        val intent = Intent(activity, OnboardingFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finishAffinity()
    }

    fun toAddPhoneContact() {
        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
        activity.startActivity(intent)
    }

    fun toEnterRestorePassword() = addFragment(EnterRestorationPasswordFragment.newInstance())

    fun toRestoreWithRecoveryPhrase() = addFragment(InputSeedWordsFragment.newInstance())

    fun toRestoreFromSeedWordsInProgress() = addFragment(WalletRestoringFromSeedWordsFragment.newInstance())

    fun onRestoreCompleted() {
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

    fun toChat() = (activity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_CHAT, NO_SMOOTH_SCROLL)

    fun toAllSettings() = (activity as HomeActivity).ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)

    fun toBackupSettings(withAnimation: Boolean) = addFragment(BackupSettingsFragment(), withAnimation = withAnimation)

    fun toDeleteWallet() = addFragment(DeleteWalletFragment())

    fun toBackgroundService() = addFragment(BackgroundServiceSettingsFragment())

    fun toMyProfile() = addFragment(WalletInfoFragment())

    fun toAbout() = addFragment(TariAboutFragment())

    fun toBackupOnboardingFlow() = addFragment(BackupOnboardingFlowFragment())

    fun toBaseNodeSelection() = addFragment(ChangeBaseNodeFragment())

    fun toTorBridges() = addFragment(TorBridgesSelectionFragment())

    fun toThemeSelection() = addFragment(ThemeSelectorFragment())

    fun toUtxos() = addFragment(UtxosListFragment())

    fun toCustomTorBridges() = addFragment(CustomTorBridgesFragment())

    fun toNetworkSelection() = addFragment(NetworkSelectionFragment())

    fun toAddCustomBaseNode() = addFragment(AddCustomBaseNodeFragment())

    fun toWalletBackupWithRecoveryPhrase() = addFragment(WriteDownSeedPhraseFragment())

    fun toSeedPhraseVerification(seedWords: List<String>) = addFragment(VerifySeedPhraseFragment.newInstance(seedWords))

    fun toConfirmPassword() = addFragment(EnterCurrentPasswordFragment())

    fun toChangePassword() = addFragment(ChangeSecurePasswordFragment())

    fun toSendTari(user: ContactDto, amount: MicroTari?) = sendToUser(user, amount)

    fun toSendWithDeeplink(deeplink: DeepLink.Send) {
        popUpTo(HomeFragment::class.java.simpleName)
        sendToUserByDeeplink(deeplink)
    }

    fun toAddContact() = addFragment(AddContactFragment())

    fun toContactDetails(contact: ContactDto) = addFragment(ContactDetailsFragment.createFragment(contact))

    fun toRequestTariFromContact(contact: ContactDto) = sendToUser(contact)

    fun toSendTariToContact(contact: ContactDto) = sendToUser(contact)

    fun backToContactBook() = popUpTo(ContactBookFragment::class.java.simpleName)

    fun backToAllSettings() = popUpTo(AllSettingsFragment::class.java.simpleName)

    fun backAfterAuth() {
        if (activity is HomeActivity) {
            popUpTo(AllSettingsFragment::class.java.simpleName)
        } else {
            popUpTo(LocalAuthFragment::class.java.simpleName)
        }
    }

    fun toLinkContact(contact: ContactDto) = addFragment(ContactLinkFragment.createFragment(contact))

    fun toContactTransactionHistory(contact: ContactDto) = addFragment(TransactionHistoryFragment.createFragment(contact))

    fun toExternalWallet(connectedWallet: YatDto.ConnectedWallet) {

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

    fun onAmountExceedsActualAvailableBalance() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(activity.string(R.string.error_balance_exceeded_title)),
                BodyModule(activity.string(R.string.error_balance_exceeded_description)),
                ButtonModule(activity.string(R.string.common_close), ButtonStyle.Close),
            )
        )
        ModularDialog(activity, args).show()
    }

    fun continueToAddNote(transactionData: TransactionData) {
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
        if (transactionData.recipientContact?.getYatDto() != null) {
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
        if (isYat) {
            activity.finish()
        } else {
            val fragmentsCount = activity.supportFragmentManager.fragments.size - 5
            for (i in 0 until fragmentsCount) {
                activity.supportFragmentManager.popBackStackImmediate()
            }
        }
    }

    fun onPasswordChanged() {
        popUpTo(BackupSettingsFragment::class.java.simpleName)
    }

    fun onSeedPhraseVerificationComplete() {
        popUpTo(BackupSettingsFragment::class.java.simpleName)
    }

    fun sendTariToUser(service: TariWalletService, sendDeeplink: DeepLink.Send) {
        val walletAddress = service.getWalletAddressFromHexString(sendDeeplink.walletAddressHex)
        sendToUser((activity as HomeActivity).viewModel.contactsRepository.ffiBridge.getContactByAddress(walletAddress))
    }

    fun sendToUserByDeeplink(deeplink: DeepLink.Send) {
        FFIWallet.instance?.getWalletAddress()
        val address = FFITariWalletAddress(HexString(deeplink.walletAddressHex)).runWithDestroy {
            walletAddressFromFFI(it)
        }
        val contact = (activity as HomeActivity).viewModel.contactsRepository.ffiBridge.getContactByAddress(address)
        val bundle = Bundle().apply {
            putSerializable(PARAMETER_CONTACT, contact)
            putParcelable(PARAMETER_AMOUNT, deeplink.amount)
        }

        addFragment(AddAmountFragment(), bundle)
    }

    private fun walletAddressFromFFI(ffiTariWalletAddress: FFITariWalletAddress): TariWalletAddress =
        TariWalletAddress(ffiTariWalletAddress.toString(), ffiTariWalletAddress.getEmojiId())

    fun sendToUser(recipientUser: ContactDto, amount: MicroTari? = null) {
        val bundle = Bundle().apply {
            putSerializable(PARAMETER_CONTACT, recipientUser)
            val innerAmount = (activity.intent.getDoubleExtra(PARAMETER_AMOUNT, Double.MIN_VALUE))
            val tariAmount = amount ?: if (innerAmount != Double.MIN_VALUE) MicroTari(BigInteger.valueOf(innerAmount.toLong())) else null
            tariAmount?.let { putParcelable(PARAMETER_AMOUNT, it) }
            activity.intent.serializable<ContactDto>(PARAMETER_CONTACT)?.let { putSerializable(PARAMETER_CONTACT, it) }
        }

        addFragment(AddAmountFragment(), bundle)
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