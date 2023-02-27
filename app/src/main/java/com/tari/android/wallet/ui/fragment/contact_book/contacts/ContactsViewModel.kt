package com.tari.android.wallet.ui.fragment.contact_book.contacts


import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.extension.toLiveData
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactsViewModel : CommonViewModel() {

    @Inject
    lateinit var repository: GIFRepository

    @Inject
    lateinit var gifRepository: GIFRepository

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var contactsRepository: ContactsRepository

    private val _navigation = SingleLiveEvent<ContactBookNavigation>()
    val navigation: LiveData<ContactBookNavigation> = _navigation

    val sourceList = MutableLiveData<MutableList<ContactItem>>(mutableListOf())

    val filters = MutableLiveData<MutableList<(ContactItem) -> Boolean>>(mutableListOf())

    val searchText = MutableLiveData("")

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    val debouncedList = Transformations.map(listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE)) {
        updateList()
    }

    init {
        component.inject(this)

        list.addSource(searchText) { updateList() }

        list.addSource(sourceList) { updateList() }

        list.addSource(filters) { updateList() }

        list.addSource(contactsRepository.publishSubject.toLiveData(BackpressureStrategy.BUFFER)) { updateTxListData() }

        doOnConnectedToWallet { doOnConnected { onServiceConnected() } }
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is ContactItem) {
            _navigation.postValue(ContactBookNavigation.ToContactDetails(item.contact))
        }
    }

    private fun onServiceConnected() {
        subscribeToEventBus()

        refreshAllData()
    }

    private fun updateTxListData() {
        val newItems = contactsRepository.publishSubject.value!!.map { ContactItem(it, 0) }.toMutableList()
        sourceList.postValue(newItems)
    }

    fun refreshAllData(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            updateList()
        }
    }

    fun search(text: String) {
        searchText.postValue(text)
    }

    fun addFilter(filter: (ContactItem) -> Boolean) {
        filters.value!!.add(filter)
        filters.value = filters.value
    }

    private fun updateList() {
        val searchText = searchText.value ?: return
        val sourceList = sourceList.value ?: return
        val filters = filters.value ?: return

        val filtered = sourceList.filter { contact -> contact.filtered(searchText) && filters.all { it.invoke(contact) } }

        val (phoneContacts, notPhoneContact)  = filtered.partition { it.contact.contact is PhoneContactDto }

        val resultList = mutableListOf<CommonViewHolderItem>()
        resultList.addAll(notPhoneContact)
        resultList.add(SettingsTitleDto(resourceManager.getString(R.string.contact_book_details_phone_contacts)))
        resultList.addAll(phoneContacts)

        list.postValue(resultList)
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onContactAddedOrUpdated(it.contactAddress, it.contactAlias) }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onContactRemoved(it.contactAddress) }
    }

    private fun onContactAddedOrUpdated(tariWalletAddress: TariWalletAddress, alias: String) {
        //todo
    }

    private fun onContactRemoved(tariWalletAddress: TariWalletAddress) {
        //todo
    }


    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
    }
}