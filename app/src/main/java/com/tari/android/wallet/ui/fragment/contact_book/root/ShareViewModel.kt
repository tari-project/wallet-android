package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.infrastructure.bluetooth.TariBluetoothClient
import com.tari.android.wallet.infrastructure.bluetooth.TariBluetoothServer
import com.tari.android.wallet.infrastructure.nfc.TariNFCAdapter
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject

class ShareViewModel : CommonViewModel() {

    @Inject
    lateinit var tariBluetoothClient: TariBluetoothClient

    @Inject
    lateinit var tariBluetoothServer: TariBluetoothServer

    @Inject
    lateinit var tariNFCAdapter: TariNFCAdapter

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var sharePrefRepository: SharedPrefsRepository

    val deeplinkViewModel = DeeplinkViewModel()

    val shareText = SingleLiveEvent<String>()

    val shareInfo = MutableLiveData<String>()

    init {
        currentInstant = this
        component.inject(this)
        tariBluetoothServer.onReceived = this::onReceived
        tariBluetoothClient.onSuccessSharing = this::showShareSuccessDialog
        tariBluetoothClient.onFailedSharing = this::showShareErrorDialog

        tariNFCAdapter.onReceived = this::onReceived
        tariNFCAdapter.onSuccessSharing = this::showShareSuccessDialog
        tariNFCAdapter.onFailedSharing = this::showShareErrorDialog
    }

    fun share(type: ShareType, deeplink: String) {
        shareInfo.postValue(deeplink)
        when (type) {
            ShareType.QR_CODE -> doShareViaQrCode(deeplink)
            ShareType.LINK -> doShareViaLink(deeplink)
            ShareType.NFC -> doShareViaNFC()
            ShareType.BLE -> doShareViaBLE()
        }
    }

    fun startBLESharing() {
        val args = ModularDialogArgs(
            DialogArgs(canceledOnTouchOutside = false, cancelable = false) { tariBluetoothClient.stopSharing() }, listOf(
                IconModule(R.drawable.vector_sharing_via_ble),
                HeadModule(resourceManager.getString(R.string.share_via_bluetooth_title)),
                BodyModule(resourceManager.getString(R.string.share_via_bluetooth_message)),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
        tariBluetoothClient.startSharing(shareInfo.value.orEmpty())
    }

    fun doContactlessPayment() {
        ensurePermissionIsGranted {
            val args = ModularDialogArgs(
                DialogArgs(canceledOnTouchOutside = false, cancelable = false) { tariBluetoothClient.stopSharing() }, listOf(
                    IconModule(R.drawable.vector_sharing_via_ble),
                    HeadModule(resourceManager.getString(R.string.contactless_payment_title)),
                    BodyModule(resourceManager.getString(R.string.contactless_payment_description)),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
                )
            )
            modularDialog.postValue(args)
            tariBluetoothClient.startDeviceScanning {
                successfullDeviceFoundSharing(it)
            }
        }
    }

    private fun ensurePermissionIsGranted(action: () -> Unit) {
        val permissions = tariBluetoothClient.bluetoothPermissions
        permissionManager.runWithPermission(permissions, action)
    }

    fun successfullDeviceFoundSharing(userProfile: DeepLink.UserProfile) {
        val contactDto = runCatching {
            val ffiWalletAddress = FFITariWalletAddress(HexString(userProfile.tariAddressHex))
            val tariWalletAddress = TariWalletAddress(ffiWalletAddress.toString(), ffiWalletAddress.getEmojiId())
            ContactDto(FFIContactDto(tariWalletAddress, userProfile.alias))
        }.getOrNull() ?: return

        val name = userProfile.alias.ifEmpty {
            contactDto.contact.extractWalletAddress().emojiId.extractEmojis().take(3).joinToString("")
        }

        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                IconModule(R.drawable.vector_sharing_via_ble),
                HeadModule(resourceManager.getString(R.string.contactless_payment_success_title)),
                BodyModule(resourceManager.getString(R.string.contactless_payment_success_description, name)),
                ButtonModule(resourceManager.getString(R.string.common_lets_do_it_2), ButtonStyle.Normal) {
                    navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(contactDto))
                    dismissDialog.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.common_no_2), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun doShareViaQrCode(deeplink: String) {
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = true), listOf(
                HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                ShareQrCodeModule(deeplink),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun doShareViaLink(deeplink: String) {
        shareText.postValue(deeplink)
        showShareSuccessDialog()
    }

    private fun doShareViaNFC() {
        if (!tariNFCAdapter.isNFCAvailable()) {
            tariNFCAdapter.showNFCSettings()
            return
        }
        val args = ModularDialogArgs(
            DialogArgs(canceledOnTouchOutside = false, cancelable = false) { tariNFCAdapter.stopSharing() }, listOf(
                IconModule(R.drawable.vector_sharing_via_nfc),
                HeadModule(resourceManager.getString(R.string.share_via_nfc_title)),
                BodyModule(resourceManager.getString(R.string.share_via_nfc_message)),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
        tariNFCAdapter.startSharing(shareInfo.value.orEmpty())
    }

    private fun doShareViaBLE() {
        permissionManager.runWithPermission(tariBluetoothServer.bluetoothPermissions) {
            startBLESharing()
        }
    }

    private fun showShareSuccessDialog() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                IconModule(R.drawable.vector_sharing_success),
                HeadModule(resourceManager.getString(R.string.share_success_title)),
                BodyModule(resourceManager.getString(R.string.share_success_message)),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun showShareErrorDialog(message: String) {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                IconModule(R.drawable.vector_sharing_failed),
                HeadModule(resourceManager.getString(R.string.common_error_title)),
                BodyModule(message),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun onReceived(data: List<DeepLink.Contacts.DeeplinkContact>) {
        deeplinkViewModel.addContacts(data)
    }

    companion object {
        var currentInstant: ShareViewModel? = null
            private set
    }
}