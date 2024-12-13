package com.tari.android.wallet.ui.screen.contactBook.link

import android.text.SpannableString
import android.text.SpannedString
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_cancel
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.common_confirm
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_title
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_title
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.getRequired
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.util.extension.switchToMain
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.contact.ContactItemViewHolderItem
import com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.emptyState.EmptyStateViewHolderItem
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.data.contacts.model.MergedContactInfo
import com.tari.android.wallet.data.contacts.model.PhoneContactInfo
import com.tari.android.wallet.ui.screen.contactBook.link.adapter.linkHeader.ContactLinkHeaderViewHolderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactLinkViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    private val _uiState = MutableStateFlow(ContactLinkModel.UiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<ContactLinkModel.Effect>()
    val effect = _effect.flow

    private val ffiContact: ContactDto = savedState.getRequired(PARAMETER_CONTACT)

    private var searchViewHolderItem: ContactLinkHeaderViewHolderItem? = null

    init {
        component.inject(this)

        collectFlow(contactsRepository.contactList) { contacts -> _uiState.update { it.copy(contacts = contacts) } }
    }

    fun onContactClick(item: ContactItemViewHolderItem) {
        showLinkDialog(item.contact)
    }

    fun grantPermission() {
        permissionManager.runWithPermission(
            permissions = listOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.WRITE_CONTACTS,
            ),
            silently = true,
        ) {
            launchOnIo {
                contactsRepository.grantContactPermissionAndRefresh()
            }
        }
    }

    val ContactLinkModel.UiState.viewItemList: List<CommonViewHolderItem>
        get() {
            if (searchViewHolderItem == null) {
                searchViewHolderItem = ContactLinkHeaderViewHolderItem(::onSearchQueryChanged, ffiContact.contactInfo.requireWalletAddress())
            }

            val listToShow = this.contacts.filter { it.contactInfo is PhoneContactInfo }
                .let { list -> if (this.searchQuery.isNotEmpty()) list.filter { it.filtered(this.searchQuery) } else list }
                .sortedBy { it.contactInfo.getAlias().lowercase() }

            return listOfNotNull(
                if (this.contacts.isEmpty()) {
                    EmptyStateViewHolderItem(
                        title = getEmptyTitle(),
                        body = getBody(this.contacts),
                        image = getEmptyImage(),
                        buttonTitle = getButtonTitle(),
                        action = { doAction() },
                    )
                } else {
                    searchViewHolderItem
                },
                *listToShow.map { contactDto -> ContactItemViewHolderItem(contact = contactDto, isSimple = true) }.toTypedArray(),
            )
        }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun getEmptyTitle(): SpannedString =
        SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_title)))

    private fun getBody(sourceList: List<ContactDto>): SpannedString {
        val noContacts = sourceList.none { it.contactInfo is PhoneContactInfo }
        val noMergedContacts = sourceList.none { it.contactInfo is MergedContactInfo }
        val havePermission = contactsRepository.contactPermissionGranted

        val resource = when {
            !havePermission -> R.string.contact_book_contacts_book_link_empty_state_no_access
            noContacts && noMergedContacts -> R.string.contact_book_contacts_book_link_empty_state_empty_book
            noContacts -> R.string.contact_book_contacts_book_link_empty_state_no_contacts
            else -> R.string.contact_book_contacts_book_link_empty_state_error
        }

        return SpannedString(HtmlHelper.getSpannedText(resourceManager.getString(resource)))
    }

    private fun getEmptyImage(): Int = R.drawable.vector_contact_favorite_empty_state

    private fun getButtonTitle(): String {
        val havePermission = contactsRepository.contactPermissionGranted

        return when {
            !havePermission -> resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_go_to_permission_settings)
            else -> resourceManager.getString(R.string.contact_book_contacts_book_link_empty_state_add_contact)
        }
    }

    private fun doAction() {
        val havePermission = contactsRepository.contactPermissionGranted

        if (!havePermission) {
            launchOnMain { _effect.send(ContactLinkModel.Effect.GrantPermission) }
        } else {
            tariNavigator.navigate(Navigation.ContactBook.ToAddPhoneContact)
        }
    }

    private fun showLinkDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.contactInfo.requireWalletAddress()
        val name = (phoneContactDto.contactInfo as PhoneContactInfo).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_secondLine, name))

        showModularDialog(
            HeadModule(resourceManager.getString(contact_book_contacts_book_link_title)),
            BodyModule(textSpannable = SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(textSpannable = SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_confirm), ButtonStyle.Normal) {
                launchOnIo {
                    contactsRepository.linkContacts(ffiContact, phoneContactDto)
                    switchToMain {
                        _uiState.update { it.copy(searchQuery = "") }
                        hideDialog()
                        showLinkSuccessDialog(phoneContactDto)
                    }
                }
            },
            ButtonModule(resourceManager.getString(common_cancel), ButtonStyle.Close)
        )
    }

    private fun showLinkSuccessDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.contactInfo.requireWalletAddress()
        val name = (phoneContactDto.contactInfo as PhoneContactInfo).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
            BodyModule(textSpannable = SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(textSpannable = SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_close), ButtonStyle.Close)
        )
        showModularDialog(ModularDialogArgs(DialogArgs {
            tariNavigator.navigate(Navigation.ContactBook.BackToContactBook)
        }, modules))
    }
}