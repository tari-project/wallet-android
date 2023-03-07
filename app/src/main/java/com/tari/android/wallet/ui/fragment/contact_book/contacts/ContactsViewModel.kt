package com.tari.android.wallet.ui.fragment.contact_book.contacts


import android.text.SpannableString
import android.text.SpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.toLiveData
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState.EmptyStateItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import io.reactivex.BackpressureStrategy
import yat.android.ui.extension.HtmlHelper
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

    val navigation = MutableLiveData<ContactBookNavigation>()

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

        list.addSource(contactsRepository.publishSubject.toLiveData(BackpressureStrategy.LATEST)) { updateContacts() }

        doOnConnectedToWallet { doOnConnected { subscribeToEventBus() } }
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is ContactItem) {
            navigation.postValue(ContactBookNavigation.ToContactDetails(item.contact))
        }
    }

    private fun updateContacts() {
        val newItems =
            contactsRepository.publishSubject.value!!.map { contactDto -> ContactItem(contactDto, false, this::performAction) { notify(it) } }
                .toMutableList()
        sourceList.postValue(newItems)
    }

    private fun notify(currentContact: ContactItem) {
        list.value?.filterIsInstance<ContactItem>()?.forEach { contactItem -> contactItem.toggleBadges(currentContact == contactItem) }
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

        val filtered = sourceList.filter { contact -> contact.filtered(searchText) && filters.all { it.invoke(contact) } }

        if (filtered.isEmpty() && searchText.isBlank()) {
            val emptyState = EmptyStateItem(getEmptyTitle(), getBody(), getEmptyImage(), getButtonTitle()) { grandPermission() }
            resultList += emptyState
        } else {
            val sorted = filtered.sortedBy { it.contact.contact.getAlias().lowercase() }

            val (phoneContacts, notPhoneContact) = sorted.partition { it.contact.contact is PhoneContactDto }

            resultList.addAll(notPhoneContact)
            if (phoneContacts.isNotEmpty()) {
                resultList.add(SettingsTitleViewHolderItem(resourceManager.getString(contact_book_details_phone_contacts)))
                resultList.addAll(phoneContacts)
            }
        }

        list.postValue(resultList)
    }

    private fun getEmptyTitle(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_title else contact_book_empty_state_title
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getBody(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_body else
            (if (isPermissionGranted()) contact_book_empty_state_body else contact_book_empty_state_body_no_permissions)
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = if (isFavorite) R.drawable.vector_contact_favorite_empty_state else R.drawable.vector_contact_empty_state

    private fun getButtonTitle(): String =
        if (isPermissionGranted()) "" else resourceManager.getString(contact_book_empty_state_grant_access_button)

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onContactAddedOrUpdated(it.contactAddress, it.contactAlias) }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onContactRemoved(it.contactAddress) }
    }

    private fun performAction(contact: ContactDto, contactAction: ContactAction) {
        when (contactAction) {
            ContactAction.Send -> navigation.postValue(ContactBookNavigation.ToSendTari(contact))
            ContactAction.ToFavorite -> contactsRepository.toggleFavorite(contact)
            ContactAction.ToUnFavorite -> contactsRepository.toggleFavorite(contact)
            ContactAction.OpenProfile -> navigation.postValue(ContactBookNavigation.ToContactDetails(contact))
            ContactAction.Link -> navigation.postValue(ContactBookNavigation.ToLinkContact(contact))
            ContactAction.Unlink -> showUnlinkDialog(contact)
            else -> Unit
        }
    }

    private fun showUnlinkDialog(contact: ContactDto) {
        val mergedDto = contact.contact as MergedContactDto
        val shortEmoji = mergedDto.ffiContactDto.walletAddress.extractShortVersion()
        val name = mergedDto.phoneContactDto.firstName
        val bodyHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_message, shortEmoji, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_title)),
            BodyModule(null, SpannableString(bodyHtml)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                contactsRepository.unlinkContact(contact)
                _dismissDialog.value = Unit
                showUnlinkSuccessDialog(contact)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showUnlinkSuccessDialog(contact: ContactDto) {
        val mergedDto = contact.contact as MergedContactDto
        val shortEmoji = mergedDto.ffiContactDto.walletAddress.extractShortVersion()
        val name = mergedDto.phoneContactDto.firstName
        val bodyHtml =
            HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_message, shortEmoji, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(bodyHtml)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) { _backPressed.postValue(Unit) },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = ContactBookNavigation.BackToContactBook()
        }, modules))
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