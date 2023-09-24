package com.tari.android.wallet.ui.fragment.home.navigation

import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.send.common.TransactionData

sealed class Navigation {

    sealed class CustomBridgeNavigation : Navigation() {
        object ScanQrCode : CustomBridgeNavigation()

        object UploadQrCode : CustomBridgeNavigation()
    }

    sealed class BackupSettingsNavigation : Navigation() {

        object ToLearnMore : BackupSettingsNavigation()

        object ToWalletBackupWithRecoveryPhrase : BackupSettingsNavigation()

        object ToChangePassword : BackupSettingsNavigation()

        object ToConfirmPassword : BackupSettingsNavigation()
    }

    sealed class VerifySeedPhraseNavigation : Navigation() {
        object ToSeedPhraseVerificationComplete : VerifySeedPhraseNavigation()

        class ToSeedPhraseVerification(val seedWords: List<String>) : VerifySeedPhraseNavigation()
    }

    sealed class BaseNodeNavigation : Navigation() {
        object ToAddCustomBaseNode : BaseNodeNavigation()
    }

    sealed class TorBridgeNavigation : Navigation() {
        object ToCustomBridges : TorBridgeNavigation()
    }

    sealed class TxListNavigation : Navigation() {

        object ToSplashScreen : TxListNavigation()

        class ToTxDetails(val tx: Tx) : TxListNavigation()

        object ToTTLStore : TxListNavigation()

        object ToAllSettings : TxListNavigation()

        object ToUtxos : TxListNavigation()

        class ToSendTariToUser(val contact: ContactDto, val amount: MicroTari? = null) : TxListNavigation()
        class ToSendWithDeeplink(val sendDeeplink: DeepLink.Send) : TxListNavigation()

        object HomeTransactionHistory : TxListNavigation()

        class ToTransfer : TxListNavigation()
    }

    sealed class AddAmountNavigation : Navigation() {
        object OnAmountExceedsActualAvailableBalance : AddAmountNavigation()

        class ContinueToAddNote(val transactionData: TransactionData) : AddAmountNavigation()

        class ContinueToFinalizing(val transactionData: TransactionData) : AddAmountNavigation()
    }

    sealed class WalletRestoringFromSeedWordsNavigation : Navigation() {
        object OnRestoreCompleted : WalletRestoringFromSeedWordsNavigation()
        object OnRestoreFailed : WalletRestoringFromSeedWordsNavigation()
    }

    sealed class AllSettingsNavigation : Navigation() {
        object ToMyProfile : AllSettingsNavigation()
        object ToBugReporting : AllSettingsNavigation()
        object ToDataCollection : AllSettingsNavigation()
        object ToAbout : AllSettingsNavigation()
        object ToBackupSettings : AllSettingsNavigation()
        object ToDeleteWallet : AllSettingsNavigation()
        object ToBackgroundService : AllSettingsNavigation()
        object ToBluetoothSettings : AllSettingsNavigation()
        object ToThemeSelection : AllSettingsNavigation()
        object ToTorBridges : AllSettingsNavigation()
        object ToNetworkSelection : AllSettingsNavigation()
        object ToBaseNodeSelection : AllSettingsNavigation()
        object ToRequestTari : AllSettingsNavigation()
    }

    sealed class InputSeedWordsNavigation : Navigation() {
        object ToRestoreFormSeedWordsInProgress : InputSeedWordsNavigation()
        object ToBaseNodeSelection : InputSeedWordsNavigation()
    }

    sealed class EnterRestorationPasswordNavigation : Navigation() {
        object OnRestore : EnterRestorationPasswordNavigation()
    }

    sealed class ChooseRestoreOptionNavigation : Navigation() {
        object ToEnterRestorePassword : ChooseRestoreOptionNavigation()

        object ToRestoreWithRecoveryPhrase : ChooseRestoreOptionNavigation()

        object OnRestoreCompleted : ChooseRestoreOptionNavigation()
    }


    sealed class ContactBookNavigation : Navigation() {

        class ToContactDetails(val contact: ContactDto) : ContactBookNavigation()

        object ToAddContact : ContactBookNavigation()

        object ToAddPhoneContact : ContactBookNavigation()

        class ToSendTari(val contact: ContactDto) : ContactBookNavigation()

        class ToSelectTariUser : ContactBookNavigation()

        class ToRequestTari(val contact: ContactDto) : ContactBookNavigation()

        class ToExternalWallet(val connectedWallet: YatDto.ConnectedWallet) : ContactBookNavigation()

        class ToLinkContact(val contact: ContactDto) : ContactBookNavigation()

        class ToContactTransactionHistory(val contact: ContactDto) : ContactBookNavigation()

        class BackToContactBook : ContactBookNavigation()
    }
}

