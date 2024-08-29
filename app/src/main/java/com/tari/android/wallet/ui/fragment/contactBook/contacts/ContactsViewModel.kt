package com.tari.android.wallet.ui.fragment.contactBook.contacts


import android.text.SpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.contact_book_details_phone_contacts
import com.tari.android.wallet.R.string.contact_book_empty_state_body
import com.tari.android.wallet.R.string.contact_book_empty_state_body_no_permissions
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_body
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_title
import com.tari.android.wallet.R.string.contact_book_empty_state_grant_access_button
import com.tari.android.wallet.R.string.contact_book_empty_state_title
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactlessPaymentItem
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.emptyState.EmptyStateItem
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.root.ContactSelectionRepository
import com.tari.android.wallet.ui.fragment.contactBook.root.ShareViewModel
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactsViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    var isFavorite = false

    val grantPermission = SingleLiveEvent<Unit>()

    val selectionTrigger: LiveData<Unit>

    val list = MediatorLiveData<List<CommonViewHolderItem>>()

    private val badgeViewModel = BadgeViewModel()

    private val sourceList = MutableLiveData<List<ContactItem>>(emptyList())

    private val filters = MutableLiveData<List<(ContactItem) -> Boolean>>(emptyList())

    private val searchText = MutableLiveData("")

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    val debouncedList = listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE).map {
        updateList()
    }

    init {
        component.inject(this)

        list.addSource(searchText) { _listUpdateTrigger.postValue(Unit) }

        list.addSource(sourceList) { _listUpdateTrigger.postValue(Unit) }

        list.addSource(filters) { _listUpdateTrigger.postValue(Unit) }

        selectionTrigger = contactSelectionRepository.isSelectionState.map { it }

        list.addSource(selectionTrigger) { _listUpdateTrigger.postValue(Unit) }

        collectFlow(contactsRepository.contactList) { updateContacts() }
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is ContactlessPaymentItem) {
            ShareViewModel.currentInstant?.doContactlessPayment()
        } else if (item is ContactItem) {
            if (contactSelectionRepository.isSelectionState.value == true) {
                contactSelectionRepository.toggle(item)
                refresh()
            } else {
                navigation.postValue(Navigation.ContactBookNavigation.ToContactDetails(item.contact))
            }
        }
    }

    fun refresh() {
        updateContacts()
        _listUpdateTrigger.postValue(Unit)
    }

    fun grantPermission() {
        permissionManager.runWithPermission(listOf(android.Manifest.permission.READ_CONTACTS), silently = false) {
            launchOnIo {
                contactsRepository.grantContactPermissionAndRefresh()
            }
        }
    }

    fun search(text: String) {
        searchText.postValue(text)
    }

    fun addFilter(filter: (ContactItem) -> Boolean) {
        filters.value = filters.value!! + filter
    }

    private fun updateContacts() {
        collectFlow(contactsRepository.contactList) { contacts ->
            val newItems = contacts.map { contactDto ->
                ContactItem(
                    contact = contactDto.copy(),
                    isSimple = false,
                    isSelectionState = false,
                    isSelected = false,
                    contactAction = { _, _ ->
                        //todo suppressed intentionally
                    },
                    badgeViewModel = badgeViewModel,
                )
            }
            launchOnMain {
                sourceList.postValue(newItems)
            }
        }
    }

    private fun updateList() {
        val searchText = searchText.value ?: return
        var sourceList = sourceList.value ?: return
        val filters = filters.value ?: return
        val selectedItems = contactSelectionRepository.selectedContacts.map { it.contact.uuid }.toList()
        sourceList = sourceList.map { it.copy() }

        val resultList = mutableListOf<CommonViewHolderItem>()
        resultList.add(ContactlessPaymentItem())

        val filtered = sourceList.filter { contact -> contact.filtered(searchText) && filters.all { it.invoke(contact) } }

        for (item in filtered) {
            item.isSelected = selectedItems.contains(item.contact.uuid)
            item.isSelectionState = contactSelectionRepository.isSelectionState.value ?: false
        }

        if (contactsRepository.contactPermissionGranted.not() || filtered.isEmpty()) {
            val emptyState = EmptyStateItem(getEmptyTitle(), getBody(), getEmptyImage(), getButtonTitle()) { grantPermission.postValue(Unit) }
            resultList += emptyState
        }

        val sorted = filtered.sortedBy { it.contact.contactInfo.getAlias().lowercase() }

        val (phoneContacts, notPhoneContact) = sorted.partition { it.contact.contactInfo is PhoneContactInfo }

        resultList.addAll(notPhoneContact)
        if (phoneContacts.isNotEmpty()) {
            resultList.add(SettingsTitleViewHolderItem(resourceManager.getString(contact_book_details_phone_contacts)))
            resultList.addAll(phoneContacts)
        }

        resultList += SpaceVerticalViewHolderItem(60)

        list.postValue(resultList)
    }

    private fun getEmptyTitle(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_title else contact_book_empty_state_title
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getBody(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_body else
            (if (contactsRepository.contactPermissionGranted) contact_book_empty_state_body else contact_book_empty_state_body_no_permissions)
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = if (isFavorite) R.drawable.vector_contact_favorite_empty_state else R.drawable.vector_contact_empty_state

    private fun getButtonTitle(): String =
        if (contactsRepository.contactPermissionGranted) "" else resourceManager.getString(contact_book_empty_state_grant_access_button)

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 200L
    }
}

