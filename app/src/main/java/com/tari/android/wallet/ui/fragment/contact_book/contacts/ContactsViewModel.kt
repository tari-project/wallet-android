package com.tari.android.wallet.ui.fragment.contact_book.contacts


import android.text.SpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.contact_book_details_phone_contacts
import com.tari.android.wallet.R.string.contact_book_empty_state_body
import com.tari.android.wallet.R.string.contact_book_empty_state_body_no_permissions
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_body
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_title
import com.tari.android.wallet.R.string.contact_book_empty_state_grant_access_button
import com.tari.android.wallet.R.string.contact_book_empty_state_title
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
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState.EmptyStateItem
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

    var isFavorite = false

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

        list.addSource(contactsRepository.publishSubject.toLiveData(BackpressureStrategy.BUFFER)) { updateContacts() }

        //todo remove
        onServiceConnected()

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

    private fun updateContacts() {
        val newItems = contactsRepository.publishSubject.value!!.map { contactDto -> ContactItem(contactDto) { notify(it) } }.toMutableList()
        sourceList.postValue(newItems)
    }

    private fun notify(currentContact: ContactItem) {
        list.value?.filterIsInstance<ContactItem>()?.forEach { contactItem -> contactItem.toggleBadges(currentContact == contactItem) }
    }

    fun refreshAllData(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContacts()
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

        val resultList = mutableListOf<CommonViewHolderItem>()

        val filtered = sourceList.filter { !it.contact.isDeleted }
            .filter { contact -> contact.filtered(searchText) && filters.all { it.invoke(contact) } }

        if (filtered.isEmpty()) {
            val emptyState = EmptyStateItem(getEmptyTitle(), getBody(), getEmptyImage(), getButtonTitle()) { grandPermission() }
            resultList += emptyState
        } else {
            val sorted = filtered.sortedBy { it.contact.contact.getAlias().lowercase() }

            val (phoneContacts, notPhoneContact) = sorted.partition { it.contact.contact is PhoneContactDto }

            resultList.addAll(notPhoneContact)
            if (phoneContacts.isNotEmpty()) {
                resultList.add(SettingsTitleDto(resourceManager.getString(contact_book_details_phone_contacts)))
                resultList.addAll(phoneContacts)
            }
        }

        list.postValue(resultList)
    }

    private fun getEmptyTitle(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_title else contact_book_empty_state_title
        return SpannedString(resourceManager.getString(resource))
    }

    private fun getBody(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_body else
            (if (isPermissionGranted()) contact_book_empty_state_body else contact_book_empty_state_body_no_permissions)
        return SpannedString(resourceManager.getString(resource))
    }

    private fun getEmptyImage(): Int = if (isFavorite) R.drawable.vector_contact_favorite_empty_state else R.drawable.vector_contact_empty_state

    private fun getButtonTitle(): String =
        if (isPermissionGranted()) "" else resourceManager.getString(contact_book_empty_state_grant_access_button)

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onContactAddedOrUpdated(it.contactAddress, it.contactAlias) }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onContactRemoved(it.contactAddress) }
    }

    private fun isPermissionGranted(): Boolean {
        //todo
        return true
    }

    private fun grandPermission(): Boolean {
        //todo
        return true
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