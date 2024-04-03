package com.tari.android.wallet.ui.fragment.contact_book.contactSelection

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkFormatter
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionModel.Effect
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionModel.YatState
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactlessPaymentItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ShareViewModel
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil.Companion.getGraphemeLength
import com.tari.android.wallet.yat.YatAdapter
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var deeplinkFormatter: DeeplinkFormatter

    @Inject
    lateinit var chatsRepository: ChatsRepository

    var additionalFilter: (ContactItem) -> Boolean = { true }

    val selectedContact = MutableLiveData<ContactDto>()

    val selectedTariWalletAddress = MutableLiveData<TariWalletAddress?>()

    private val contactListSource = MediatorLiveData<List<ContactItem>>()

    private val searchText = MutableLiveData("")

    val contactList = MediatorLiveData<List<CommonViewHolderItem>>()

    val clipboardChecker = MediatorLiveData<Unit>()

    val walletAddressViewModel = WalletAddressViewModel()

    val isContactlessPayment = MutableLiveData(false)

    val amount: MutableLiveData<MicroTari> = MutableLiveData()

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    private val _yatState = MutableStateFlow(YatState())
    val yatState = _yatState.asStateFlow()

    init {
        component.inject(this)

        doOnConnected {
            walletAddressViewModel.checkClipboardForValidEmojiId(it)
        }

        contactListSource.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) {
            contactListSource.value = it.filter(contactsRepository.filter).map { contactDto -> ContactItem(contactDto, true) }
        }

        contactList.addSource(contactListSource) { updateContactList() }
        contactList.addSource(searchText) { updateContactList() }
        contactList.addSource(isContactlessPayment) { updateContactList() }
    }

    fun handleDeeplink(deeplinkString: String) {
        val deeplink = deeplinkFormatter.parse(deeplinkString)
        val hex = when (deeplink) {
            is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.hex
            is DeepLink.Send -> deeplink.walletAddressHex
            is DeepLink.UserProfile -> deeplink.tariAddressHex
            else -> null
        }.orEmpty()

        val name = when (deeplink) {
            is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.alias
            is DeepLink.UserProfile -> deeplink.alias
            else -> null
        }.orEmpty()

        when (deeplink) {
            is DeepLink.Send -> deeplink.amount?.let { amount.value = it }
            else -> Unit
        }

        if (hex.isEmpty()) return
        val walletAddress = walletService.getWalletAddressFromHexString(hex)
        selectedContact.value = ContactDto(FFIContactDto(walletAddress), name)
        _yatState.update { it.copy(yatUser = null) }
    }

    fun getUserDto(): ContactDto =
        yatState.value.yatUser?.let {
            ContactDto(
                contact = FFIContactDto(it.walletAddress),
                yat = YatDto(
                    yat = it.yatName,
                    connectedWallets = it.connectedWallets,
                ),
            )
        }
            ?: selectedContact.value
            ?: contactListSource.value.orEmpty().firstOrNull { it.contact.contact.extractWalletAddress() == selectedTariWalletAddress.value }?.contact
            ?: ContactDto(FFIContactDto(selectedTariWalletAddress.value!!))

    fun onContactlessPaymentClick() {
        ShareViewModel.currentInstant?.doContactlessPayment()
    }

    fun onContactClick(contact: ContactDto) {
        selectedContact.value = contact
        _yatState.update { it.copy(yatUser = null) }
    }

    fun deselectTariWalletAddress() {
        selectedTariWalletAddress.value = null
    }

    private fun updateContactList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        var list = source.filter { additionalFilter.invoke(it) }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        val result = mutableListOf<CommonViewHolderItem>()

        if (isContactlessPayment.value == true) {
            result.add(ContactlessPaymentItem())
        }

        val resentUsed = list.filter { it.contact.lastUsedDate != null }
            .sortedBy { item -> item.contact.lastUsedDate?.date }
            .take(Constants.Contacts.recentContactCount)

        if (resentUsed.isNotEmpty()) {
            result.add(TitleViewHolderItem(resourceManager.getString(R.string.add_recipient_recent_tx_contacts)))
        }
        result.addAll(resentUsed)

        val restOfContact = list.filter { !resentUsed.contains(it) }.sortedBy { it.contact.contact.getAlias().lowercase() }
        if (restOfContact.isNotEmpty() && resentUsed.isNotEmpty()) {
            result.add(TitleViewHolderItem(resourceManager.getString(R.string.add_recipient_my_contacts)))
        }

        result.addAll(restOfContact)

        this.contactList.postValue(result)
    }

    fun toggleYatEye() {
        _yatState.update { it.toggleEye() }
    }

    fun addressEntered(addressText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val yatUser = tryToFindYatUser(addressText)
            _yatState.update { it.copy(yatUser = yatUser) }
            val walletAddress = walletAddressViewModel.getWalletAddressFromEmojiId(addressText)
            selectedTariWalletAddress.postValue(walletAddress)

            if (yatUser == null && walletAddress == null) {
                searchText.postValue(addressText)
                if (addressText.isNotEmpty()) {
                    _effect.send(Effect.ShowNotValidEmojiId)
                }
            } else {
                _effect.send(Effect.ShowNextButton)
            }
        }
    }

    private fun tryToFindYatUser(emojiId: String): YatState.YatUser? {
        if (emojiId.isEmpty() || emojiId.getGraphemeLength() > YAT_MAX_LENGTH) return null

        return yatAdapter.searchTariYats(emojiId)?.result?.entries?.let { resultEntries ->
            resultEntries.firstOrNull()?.value?.address?.let { hexAddress ->
                YatState.YatUser(
                    yatName = emojiId,
                    hexAddress = hexAddress,
                    walletAddress = walletService.getWalletAddressFromHexString(hexAddress),
                    connectedWallets = resultEntries.map { YatDto.ConnectedWallet(it.key, it.value) }
                )
            }
        }
    }

    companion object {
        private const val YAT_MAX_LENGTH = 5
    }
}


