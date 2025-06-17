package com.tari.android.wallet.ui.screen.contactBook.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.infrastructure.ShareManager
import com.tari.android.wallet.infrastructure.ShareType
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.screen.contactBook.root.share.ShareOptionArgs
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    @Inject
    lateinit var shareManager: ShareManager

    val shareList = MutableLiveData<List<ShareOptionArgs>>()

    val walletAddressViewModel = WalletAddressViewModel()

    val query = MutableLiveData<String>()

    init {
        component.inject(this)

        val list = mutableListOf<ShareOptionArgs>()
        list.add(
            ShareOptionArgs(
                type = ShareType.QR_CODE,
                title = resourceManager.getString(R.string.share_contact_via_qr_code),
                icon = R.drawable.vector_share_qr_code,
                isSelected = true,
            ) { shareViaQrCode() }
        )
        list.add(
            ShareOptionArgs(
                type = ShareType.LINK,
                title = resourceManager.getString(R.string.share_contact_via_qr_link),
                icon = R.drawable.vector_share_link,
            ) { shareViaLink() }
        )

        shareList.postValue(list)
    }

    override fun handleDeeplink(deeplink: DeepLink) {
        if (deeplink is DeepLink.Contacts || deeplink is DeepLink.Send || deeplink is DeepLink.UserProfile) {
            when (deeplink) {
                is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.tariAddress
                is DeepLink.Send -> deeplink.walletAddress
                is DeepLink.UserProfile -> deeplink.tariAddress
                else -> null
            }?.let { TariWalletAddress.fromBase58OrNull(it) }?.let { query.postValue(it.fullEmojiId) }
        } else {
            super.handleDeeplink(deeplink)
        }
    }

    fun doSearch(query: String) {
        walletAddressViewModel.checkQueryForValidEmojiId(query)
    }

    fun send() {
        val walletAddress = walletAddressViewModel.discoveredWalletAddressFromQuery.value!!
        val contact = contactsRepository.findOrCreateContact(walletAddress)
        tariNavigator.navigate(Navigation.TxList.ToSendTariToUser(contact))
    }

    fun shareSelectedContacts() {
        val args = shareList.value!!.first { it.isSelected }
        val selectedContacts = contactSelectionRepository.selectedContacts.map { it.contact }
        contactSelectionRepository.clear()
        val deeplink = getDeeplink(selectedContacts)
        shareManager.share(dialogManager = this, type = args.type, deeplink = deeplink)
    }

    private fun shareViaQrCode() = setSelectedToShareType(ShareType.QR_CODE)

    private fun shareViaLink() = setSelectedToShareType(ShareType.LINK)

    private fun getDeeplink(selectedContacts: List<Contact>): String {
        val contacts = selectedContacts.map {
            DeepLink.Contacts.DeeplinkContact(
                alias = it.alias.orEmpty(),
                tariAddress = it.walletAddress.fullBase58,
            )
        }
        return deeplinkManager.getDeeplinkString(DeepLink.Contacts(contacts))
    }

    private fun setSelectedToShareType(shareType: ShareType) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values.firstOrNull { it.type == shareType }?.isSelected = true
        shareList.postValue(values)
    }
}
