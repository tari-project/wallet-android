package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareType
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var deeplinkFormatter: DeeplinkHandler

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    val shareViewModel = ShareViewModel()

    val shareList = MutableLiveData<List<ShareOptionArgs>>()

    val readyToSend = MutableLiveData<String>()

    val walletAddressViewModel = WalletAddressViewModel()

    init {
        component.inject(this)

        shareList.postValue(
            listOf(
                ShareOptionArgs(
                    ShareType.QR_CODE,
                    resourceManager.getString(R.string.share_contact_via_qr_code),
                    R.drawable.vector_share_qr_code,
                    true
                ) { shareViaQrCode() },
                ShareOptionArgs(
                    ShareType.LINK,
                    resourceManager.getString(R.string.share_contact_via_qr_link),
                    R.drawable.vector_share_link
                ) { shareViaLink() },
                ShareOptionArgs(
                    ShareType.NFC,
                    resourceManager.getString(R.string.share_contact_via_qr_nfc),
                    R.drawable.vector_share_nfc
                ) { shareViaNFC() },
                ShareOptionArgs(
                    ShareType.BLE,
                    resourceManager.getString(R.string.share_contact_via_qr_ble),
                    R.drawable.vector_share_ble
                ) { shareViaBLE() },
            )
        )

        shareViewModel.tariBluetoothServer.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, action)
        }

        shareViewModel.tariBluetoothClient.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, action)
        }
    }

    fun doSearch(query: String) {
        readyToSend.postValue(query)
    }

    fun send() {
//        val tariWalletAddress = walletService.contact
//        val contact = contactsRepository.ffiBridge.getContactByAdress()
//        navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser())
        TODO("Not yet implemented")
    }

    fun shareViaQrCode() = setSelectedToPosition(0)

    fun shareViaLink() = setSelectedToPosition(1)

    fun shareViaNFC() = setSelectedToPosition(2)

    fun shareViaBLE() = setSelectedToPosition(3)

    fun shareSelectedContacts() {
        val args = shareList.value!!.first { it.isSelected }
        val selectedContacts = contactSelectionRepository.selectedContacts.map { it.contact }
        contactSelectionRepository.clear()
        val deeplink = getDeeplink(selectedContacts)
        shareViewModel.share(args.type, deeplink)
    }

    private fun getDeeplink(selectedContacts: List<ContactDto>) : String {
        val contacts = selectedContacts.map { DeepLink.Contacts.DeeplinkContact(it.contact.getAlias(), it.contact.extractWalletAddress().hexString) }
        return deeplinkFormatter.getDeeplink(DeepLink.Contacts(contacts))
    }

    private fun setSelectedToPosition(position: Int) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values[position].isSelected = true
        shareList.postValue(values)
    }
}


