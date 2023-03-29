package com.tari.android.wallet.ui.fragment.contact_book.contacts


import android.text.SpannableString
import android.text.SpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.contact_book_details_phone_contacts
import com.tari.android.wallet.R.string.contact_book_empty_state_body
import com.tari.android.wallet.R.string.contact_book_empty_state_body_no_permissions
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_body
import com.tari.android.wallet.R.string.contact_book_empty_state_favorites_title
import com.tari.android.wallet.R.string.contact_book_empty_state_grant_access_button
import com.tari.android.wallet.R.string.contact_book_empty_state_title
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState.EmptyStateItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
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

    val badgeViewModel = BadgeViewModel()

    val grantPermission = SingleLiveEvent<Unit>()

    val sourceList = MutableLiveData<MutableList<ContactItem>>(mutableListOf())

    val filters = MutableLiveData<MutableList<(ContactItem) -> Boolean>>(mutableListOf())

    val searchText = MutableLiveData("")

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    val debouncedList = listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE).map {
        updateList()
    }

    init {
        component.inject(this)

        list.addSource(searchText) { updateList() }

        list.addSource(sourceList) { updateList() }

        list.addSource(filters) { updateList() }

        list.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) { updateContacts() }
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is ContactItem) {
            navigation.postValue(Navigation.ContactBookNavigation.ToContactDetails(item.contact))
        }
    }

    private fun updateContacts() {
        val newItems =
            contactsRepository.publishSubject.value!!.map { contactDto -> ContactItem(contactDto, false, this::performAction, badgeViewModel) }
                .toMutableList()
        sourceList.postValue(newItems)
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

        if (contactsRepository.contactPermission.value == false || filtered.isEmpty()) {
            val emptyState = EmptyStateItem(getEmptyTitle(), getBody(), getEmptyImage(), getButtonTitle()) { grantPermission.postValue(Unit) }
            resultList += emptyState
        }

        val sorted = filtered.sortedBy { it.contact.contact.getAlias().lowercase() }

        val (phoneContacts, notPhoneContact) = sorted.partition { it.contact.contact is PhoneContactDto }

        resultList.addAll(notPhoneContact)
        if (phoneContacts.isNotEmpty()) {
            resultList.add(SettingsTitleViewHolderItem(resourceManager.getString(contact_book_details_phone_contacts)))
            resultList.addAll(phoneContacts)
        }

        list.postValue(resultList)
    }

    private fun getEmptyTitle(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_title else contact_book_empty_state_title
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getBody(): SpannedString {
        val resource = if (isFavorite) contact_book_empty_state_favorites_body else
            (if (contactsRepository.contactPermission.value == true) contact_book_empty_state_body else contact_book_empty_state_body_no_permissions)
        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = if (isFavorite) R.drawable.vector_contact_favorite_empty_state else R.drawable.vector_contact_empty_state

    private fun getButtonTitle(): String =
        if (contactsRepository.contactPermission.value == true) "" else resourceManager.getString(contact_book_empty_state_grant_access_button)

    private fun performAction(contact: ContactDto, contactAction: ContactAction) {
        when (contactAction) {
            ContactAction.Send -> navigation.postValue(Navigation.ContactBookNavigation.ToSendTari(contact))
            ContactAction.ToFavorite -> contactsRepository.toggleFavorite(contact)
            ContactAction.ToUnFavorite -> contactsRepository.toggleFavorite(contact)
            ContactAction.OpenProfile -> navigation.postValue(Navigation.ContactBookNavigation.ToContactDetails(contact))
            ContactAction.Link -> navigation.postValue(Navigation.ContactBookNavigation.ToLinkContact(contact))
            ContactAction.Unlink -> showUnlinkDialog(contact)
            else -> Unit
        }
    }

    private fun showUnlinkDialog(contact: ContactDto) {
        val mergedDto = contact.contact as MergedContactDto
        val walletAddress = mergedDto.ffiContactDto.walletAddress
        val name = mergedDto.phoneContactDto.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
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
        val walletAddress = mergedDto.ffiContactDto.walletAddress
        val name = mergedDto.phoneContactDto.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_message_firstLine))
        val secondLineHtml =
            HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = Navigation.ContactBookNavigation.BackToContactBook()
        }, modules))
    }

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
    }
}

