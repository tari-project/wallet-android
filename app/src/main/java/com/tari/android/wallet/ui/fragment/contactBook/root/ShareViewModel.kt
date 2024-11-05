package com.tari.android.wallet.ui.fragment.contactBook.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.infrastructure.bluetooth.TariBluetoothClient
import com.tari.android.wallet.infrastructure.bluetooth.TariBluetoothServer
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
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.util.ContactUtil
import javax.inject.Inject

// TODO make it not VM, but a singleton service
class ShareViewModel : CommonViewModel() {

    @Inject
    lateinit var tariBluetoothClient: TariBluetoothClient

    @Inject
    lateinit var tariBluetoothServer: TariBluetoothServer

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var contactUtil: ContactUtil

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    val shareText = SingleLiveEvent<String>()

    val shareInfo = MutableLiveData<String>()

    init {
        currentInstant = this
        component.inject(this)
        tariBluetoothServer.onReceived = this::onReceived
        tariBluetoothClient.onSuccessSharing = this::showShareSuccessDialog
        tariBluetoothClient.onFailedSharing = this::showShareErrorDialog
    }

    fun share(type: ShareType, deeplink: String) {
        shareInfo.postValue(deeplink)
        when (type) {
            ShareType.QR_CODE -> doShareViaQrCode(deeplink)
            ShareType.LINK -> doShareViaLink(deeplink)
            ShareType.BLE -> doShareViaBLE()
        }
    }

    fun startBLESharing() {
        showModularDialog(
            ModularDialogArgs(
                DialogArgs(canceledOnTouchOutside = false, cancelable = false) { tariBluetoothClient.stopSharing() }, listOf(
                    IconModule(R.drawable.vector_sharing_via_ble),
                    HeadModule(resourceManager.getString(R.string.share_via_bluetooth_title)),
                    BodyModule(resourceManager.getString(R.string.share_via_bluetooth_message)),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
        tariBluetoothClient.startSharing(shareInfo.value.orEmpty())
    }

    fun doContactlessPayment() {
        permissionManager.runWithPermission(tariBluetoothClient.bluetoothPermissions) {
            showModularDialog(
                ModularDialogArgs(
                    DialogArgs(canceledOnTouchOutside = false, cancelable = false) { tariBluetoothClient.stopSharing() }, listOf(
                        IconModule(R.drawable.vector_sharing_via_ble),
                        HeadModule(resourceManager.getString(R.string.contactless_payment_title)),
                        BodyModule(resourceManager.getString(R.string.contactless_payment_description)),
                        ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                    )
                )
            )
            tariBluetoothClient.startDeviceScanning {
                successfulDeviceFoundSharing(it)
            }
        }
    }

    private fun successfulDeviceFoundSharing(userProfile: DeepLink.UserProfile) {
        val contactDto = runCatching {
            ContactDto(FFIContactInfo(walletAddress = TariWalletAddress.fromBase58(userProfile.tariAddress), alias = userProfile.alias))
        }.getOrNull() ?: return

        val name = contactUtil.normalizeAlias(userProfile.alias, contactDto.contactInfo.requireWalletAddress())

        showModularDialog(
            IconModule(R.drawable.vector_sharing_via_ble),
            HeadModule(resourceManager.getString(R.string.contactless_payment_success_title)),
            BodyModule(resourceManager.getString(R.string.contactless_payment_success_description, name)),
            ButtonModule(resourceManager.getString(R.string.common_lets_do_it_2), ButtonStyle.Normal) {
                navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(contactDto))
                hideDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_no_2), ButtonStyle.Close),
        )
    }

    private fun doShareViaQrCode(deeplink: String) {
        showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = true), listOf(
                    HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                    ShareQrCodeModule(deeplink),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
    }

    private fun doShareViaLink(deeplink: String) {
        shareText.postValue(deeplink)
        showShareSuccessDialog()
    }

    private fun doShareViaBLE() {
        permissionManager.runWithPermission(tariBluetoothServer.bluetoothPermissions) {
            startBLESharing()
        }
    }

    private fun showShareSuccessDialog() {
        showModularDialog(
            IconModule(R.drawable.vector_sharing_success),
            HeadModule(resourceManager.getString(R.string.share_success_title)),
            BodyModule(resourceManager.getString(R.string.share_success_message)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
        )
    }

    private fun showShareErrorDialog(message: String) {
        showModularDialog(
            IconModule(R.drawable.vector_sharing_failed),
            HeadModule(resourceManager.getString(R.string.common_error_title)),
            BodyModule(message),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
        )
    }

    private fun onReceived(data: List<DeepLink.Contacts.DeeplinkContact>) {
        HomeActivity.instance.get()?.let { context ->
            deeplinkManager.execute(context, DeepLink.Contacts(data))
        }
    }

    companion object {
        var currentInstant: ShareViewModel? = null
            private set
    }
}