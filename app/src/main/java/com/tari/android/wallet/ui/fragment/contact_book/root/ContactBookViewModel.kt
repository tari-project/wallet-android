package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkFormatter
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.infrastructure.nfc.TariNFCAdapter
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    @Inject
    lateinit var nfcAdapter: TariNFCAdapter

    @Inject
    lateinit var deeplinkFormatter: DeeplinkFormatter

    val shareList = MutableLiveData<List<ShareOptionArgs>>()

    val walletAddressViewModel = WalletAddressViewModel()

    val query = MutableLiveData<String>()

    init {
        component.inject(this)

        val list = mutableListOf<ShareOptionArgs>()
        list.add(
            ShareOptionArgs(
                ShareType.QR_CODE,
                resourceManager.getString(R.string.share_contact_via_qr_code),
                R.drawable.vector_share_qr_code,
                true
            ) { shareViaQrCode() }
        )
        list.add(
            ShareOptionArgs(
                ShareType.LINK,
                resourceManager.getString(R.string.share_contact_via_qr_link),
                R.drawable.vector_share_link
            ) { shareViaLink() }
        )
        if (nfcAdapter.isNFCSupported()) {
            list.add(
                ShareOptionArgs(
                    ShareType.NFC,
                    resourceManager.getString(R.string.share_contact_via_qr_nfc),
                    R.drawable.vector_share_nfc
                ) { shareViaNFC() }
            )
        }
        list.add(
            ShareOptionArgs(
                ShareType.BLE,
                resourceManager.getString(R.string.share_contact_via_qr_ble),
                R.drawable.vector_share_ble
            ) { shareViaBLE() }
        )

        shareList.postValue(list)
    }

    fun handleDeeplink(deeplinkString: String) {
        val deeplink = deeplinkFormatter.parse(deeplinkString)
        val hex = when (deeplink) {
            is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.hex
            is DeepLink.Send -> deeplink.walletAddressHex
            is DeepLink.UserProfile -> deeplink.tariAddressHex
            else -> null
        }.orEmpty()

        if (hex.isEmpty()) return
        val walletAddress = walletService.getWalletAddressFromHexString(hex)
        query.postValue(walletAddress.emojiId)
    }

    fun doSearch(query: String) {
        doOnConnected {
            walletAddressViewModel.checkFromQuery(it, query)
        }
    }

    fun send() {
        val walletAddress = walletAddressViewModel.discoveredWalletAddressFromQuery.value!!
        val contact = contactsRepository.ffiBridge.getContactByAddress(walletAddress)
        navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(contact))
    }

    fun shareViaQrCode() = setSelectedToShareType(ShareType.QR_CODE)

    fun shareViaLink() = setSelectedToShareType(ShareType.LINK)

    fun shareViaNFC() = setSelectedToShareType(ShareType.NFC)

    fun shareViaBLE() = setSelectedToShareType(ShareType.BLE)

    fun shareSelectedContacts() {
        val args = shareList.value!!.first { it.isSelected }
        val selectedContacts = contactSelectionRepository.selectedContacts.map { it.contact }
        contactSelectionRepository.clear()
        val deeplink = getDeeplink(selectedContacts)
        ShareViewModel.currentInstant?.share(args.type, deeplink)
    }

    private fun getDeeplink(selectedContacts: List<ContactDto>): String {
        val contacts = selectedContacts.map { DeepLink.Contacts.DeeplinkContact(ContactDto.normalizeAlias(it.contact.getAlias(), it.contact.extractWalletAddress()), it.contact.extractWalletAddress().hexString) }
        return deeplinkHandler.getDeeplink(DeepLink.Contacts(contacts))
    }

    private fun setSelectedToShareType(shareType: ShareType) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values.firstOrNull { it.type == shareType }?.isSelected = true
        shareList.postValue(values)
    }
}


