package com.tari.android.wallet.ui.screen.contactBook.contacts


import android.text.SpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.contact_book_details_phone_contacts
import com.tari.android.wallet.R.string.contact_book_empty_state_body_no_permissions
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_body
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_title
import com.tari.android.wallet.R.string.contact_book_empty_state_grant_access_button
import com.tari.android.wallet.R.string.contact_book_empty_state_title
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.contact.ContactItemViewHolderItem
import com.tari.android.wallet.ui.screen.contactBook.root.ContactSelectionRepository
import com.tari.android.wallet.ui.screen.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.debounce
import com.tari.android.wallet.util.extension.launchOnMain
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactsViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var contactSelectionRepository: ContactSelectionRepository

    var isFavorite = false

    val selectionTrigger: LiveData<Unit>

    val contactList = MediatorLiveData<List<CommonViewHolderItem>>()

    private val sourceList = MutableLiveData<List<ContactItemViewHolderItem>>(emptyList())

    private val filters = MutableLiveData<List<(ContactItemViewHolderItem) -> Boolean>>(emptyList())

    private val searchText = MutableLiveData("")

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    val debouncedList = listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE).map {
        updateList()
    }

    init {
        component.inject(this)

        contactList.addSource(searchText) { _listUpdateTrigger.postValue(Unit) }

        contactList.addSource(sourceList) { _listUpdateTrigger.postValue(Unit) }

        contactList.addSource(filters) { _listUpdateTrigger.postValue(Unit) }

        selectionTrigger = contactSelectionRepository.isSelectionState.map { it }

        contactList.addSource(selectionTrigger) { _listUpdateTrigger.postValue(Unit) }

        collectFlow(contactsRepository.contactList) { updateContacts(it) }
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is ContactItemViewHolderItem) {
            if (contactSelectionRepository.isSelectionState.value == true) {
                contactSelectionRepository.toggle(item)
                refresh()
            } else {
                tariNavigator.navigate(Navigation.ContactBook.ToContactDetails(item.contact))
            }
        }
    }

    fun refresh() {
        updateContacts(contactsRepository.contactList.value)
        _listUpdateTrigger.postValue(Unit)
    }

    fun search(text: String) {
        searchText.postValue(text)
    }

    fun addFilter(filter: (ContactItemViewHolderItem) -> Boolean) {
        filters.value = filters.value!! + filter
    }

    private fun updateContacts(contacts: List<Contact>) {
        val newItems = contacts.map { contactDto ->
            ContactItemViewHolderItem(
                contact = contactDto.copy(),
                isSimple = false,
                isSelectionState = false,
                isSelected = false,
            )
        }
        launchOnMain {
            sourceList.postValue(newItems)
        }
    }

    private fun updateList() {
        searchText.value ?: return
        var sourceList = sourceList.value ?: return
        filters.value ?: return
        val selectedItems = contactSelectionRepository.selectedContacts.map { it.contact.walletAddress }.toList()
        sourceList = sourceList.map { it.copy() }

        val filtered = sourceList

        for (item in filtered) {
            item.isSelected = selectedItems.contains(item.contact.walletAddress)
            item.isSelectionState = contactSelectionRepository.isSelectionState.value == true
        }

        if (filtered.isEmpty()) {

        } else {
            val (phoneContacts, notPhoneContacts) = emptyList<ContactItemViewHolderItem>() to filtered

            contactList.postValue(
                listOfNotNull(
                    *notPhoneContacts.toTypedArray(),

                    SettingsTitleViewHolderItem(resourceManager.getString(contact_book_details_phone_contacts)).takeIf { phoneContacts.isNotEmpty() },
                    *phoneContacts.toTypedArray(),

                    SpaceVerticalViewHolderItem(60),
                )
            )
        }
    }

    private fun getEmptyTitle(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_title else contact_book_empty_state_title
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyBody(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_body else
            (contact_book_empty_state_body_no_permissions)
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = if (isFavorite) R.drawable.vector_contact_favorite_empty_state else R.drawable.vector_contact_empty_state

    private fun getButtonTitle(): String = resourceManager.getString(contact_book_empty_state_grant_access_button)

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 200L
    }
}

