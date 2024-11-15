package com.tari.android.wallet.navigation

import android.net.Uri
import com.tari.android.wallet.application.YatAdapter.ConnectedWallet
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.pinCode.PinCodeScreenBehavior
import com.tari.android.wallet.ui.fragment.send.common.TransactionData

sealed class Navigation {

    class EnterPinCodeNavigation(val behavior: PinCodeScreenBehavior, val stashedPin: String? = null) : Navigation()
    object ChangeBiometrics : Navigation()
    object FeatureAuth : Navigation()
    data class SplashScreen(val seedWords: List<String>? = null, val clearTop: Boolean = true) : Navigation()
    data class Home(val uri: Uri? = null) : Navigation()
    data object BackToHome : Navigation()

    sealed class CustomBridgeNavigation : Navigation() {
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

    sealed class TorBridgeNavigation : Navigation() {
        object ToCustomBridges : TorBridgeNavigation()
    }

    sealed class TxListNavigation : Navigation() {
        class ToTxDetails(val tx: Tx) : TxListNavigation()
        object ToChat : TxListNavigation()
        object ToAllSettings : TxListNavigation()
        object ToUtxos : TxListNavigation()
        class ToSendTariToUser(val contact: ContactDto, val amount: MicroTari? = null) : TxListNavigation()
        class ToSendWithDeeplink(val sendDeeplink: DeepLink.Send) : TxListNavigation()
        object HomeTransactionHistory : TxListNavigation()
        object ToTransfer : TxListNavigation()
    }

    sealed class ChatNavigation : Navigation() {
        object ToAddChat : ChatNavigation()
        class ToChat(val walletAddress: TariWalletAddress, val isNew: Boolean) : ChatNavigation()
    }

    sealed class AddAmountNavigation : Navigation() {
        object OnAmountExceedsActualAvailableBalance : AddAmountNavigation()
        class ContinueToAddNote(val transactionData: TransactionData) : AddAmountNavigation()
        class ContinueToFinalizing(val transactionData: TransactionData) : AddAmountNavigation()
    }

    sealed class AllSettingsNavigation : Navigation() {
        object ToMyProfile : AllSettingsNavigation()
        object ToBugReporting : AllSettingsNavigation()
        object ToDataCollection : AllSettingsNavigation()
        object ToAbout : AllSettingsNavigation()
        object ToBackupSettings : AllSettingsNavigation()
        object ToDeleteWallet : AllSettingsNavigation()
        object ToBackgroundService : AllSettingsNavigation()
        object ToScreenRecording : AllSettingsNavigation()
        object ToBluetoothSettings : AllSettingsNavigation()
        object ToThemeSelection : AllSettingsNavigation()
        object ToTorBridges : AllSettingsNavigation()
        object ToNetworkSelection : AllSettingsNavigation()
        object ToBaseNodeSelection : AllSettingsNavigation()
        object ToRequestTari : AllSettingsNavigation()
    }

    sealed class InputSeedWordsNavigation : Navigation() {
        object ToRestoreFromSeeds : InputSeedWordsNavigation()
        object ToBaseNodeSelection : InputSeedWordsNavigation()
    }

    sealed class ChooseRestoreOptionNavigation : Navigation() {
        object ToEnterRestorePassword : ChooseRestoreOptionNavigation()
        data object ToRestoreWithRecoveryPhrase : ChooseRestoreOptionNavigation()
    }

    sealed class ContactBookNavigation : Navigation() {
        class ToContactDetails(val contact: ContactDto) : ContactBookNavigation()
        object ToAddContact : ContactBookNavigation()
        object ToAddPhoneContact : ContactBookNavigation()
        class ToSendTari(val contact: ContactDto) : ContactBookNavigation()
        object ToSelectTariUser : ContactBookNavigation()
        class ToRequestTari(val contact: ContactDto) : ContactBookNavigation()
        class ToExternalWallet(val connectedWallet: ConnectedWallet) : ContactBookNavigation()
        class ToLinkContact(val contact: ContactDto) : ContactBookNavigation()
        class ToContactTransactionHistory(val contact: ContactDto) : ContactBookNavigation()
        object BackToContactBook : ContactBookNavigation()
    }
}
