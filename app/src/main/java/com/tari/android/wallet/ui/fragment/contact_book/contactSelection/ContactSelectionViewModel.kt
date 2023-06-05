package com.tari.android.wallet.ui.fragment.contact_book.contactSelection

import android.content.ClipboardManager
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.yat.YatAdapter
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject

open class ContactSelectionViewModel : CommonViewModel() {

    private var searchingJob: Job? = null

    var additionalFilter: (ContactItem) -> Boolean = { true }

    val selectedUser = MutableLiveData<ContactDto>()

    val selectedTariWalletAddress = MutableLiveData<TariWalletAddress>()

    val contactListSource = MediatorLiveData<List<ContactItem>>()

    val searchText = MutableLiveData("")

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val clipboardChecker = MediatorLiveData<Unit>()

    val foundYatUser: SingleLiveEvent<Optional<YatDto>> = SingleLiveEvent()

    val walletAddressViewModel = WalletAddressViewModel()

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    init {
        component.inject(this)

        doOnConnected {
            walletAddressViewModel.checkClipboardForValidEmojiId(it)
        }

        contactListSource.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) {
            contactListSource.value = it.filter(contactsRepository.filter).map { contactDto -> ContactItem(contactDto, true) }
        }

        list.addSource(contactListSource) { updateList() }
        list.addSource(searchText) { updateList() }
    }

    fun getUserDto(): ContactDto = selectedUser.value ?: contactListSource.value.orEmpty()
        .firstOrNull { it.contact.contact.extractWalletAddress() == selectedTariWalletAddress.value }?.contact
    ?: ContactDto(FFIContactDto(selectedTariWalletAddress.value!!))

    private fun updateList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        searchAndDisplayRecipients(searchText)

        var list = source.filter { additionalFilter.invoke(it) }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        val result = mutableListOf<CommonViewHolderItem>()

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

        this.list.postValue(result)
    }

    private fun searchAndDisplayRecipients(query: String) {
        searchingJob?.cancel()
        foundYatUser.value = Optional.ofNullable(null)

        if (query.isEmpty()) return

        searchingJob = viewModelScope.launch(Dispatchers.IO) {
            yatAdapter.searchTariYats(query)?.result?.entries?.firstOrNull()?.let { response ->
                walletService.getWalletAddressFromHexString(response.value.address)?.let { pubKey ->
                    val yatUser = YatDto(query)
                    foundYatUser.postValue(Optional.ofNullable(yatUser))
                }
            }
        }
    }
}


