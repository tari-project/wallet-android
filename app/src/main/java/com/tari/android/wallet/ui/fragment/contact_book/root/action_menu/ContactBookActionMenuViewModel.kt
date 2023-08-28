package com.tari.android.wallet.ui.fragment.contact_book.root.action_menu

import android.text.SpannableString
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactBookActionMenuViewModel : CommonViewModel() {

    val showWithContact = SingleLiveEvent<ContactDto>()

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }

    fun doAction(contactAction: ContactAction, contact: ContactDto) {
        performAction(contact, contactAction)
    }

    private fun performAction(contact: ContactDto, contactAction: ContactAction) {
        when (contactAction) {
            ContactAction.Send -> navigation.postValue(Navigation.ContactBookNavigation.ToSendTari(contact))
            ContactAction.ToFavorite -> runWithUpdate { contactsRepository.toggleFavorite(contact) }
            ContactAction.ToUnFavorite -> runWithUpdate { contactsRepository.toggleFavorite(contact) }
            ContactAction.OpenProfile -> navigation.postValue(Navigation.ContactBookNavigation.ToContactDetails(contact))
            ContactAction.Link -> navigation.postValue(Navigation.ContactBookNavigation.ToLinkContact(contact))
            ContactAction.Unlink -> showUnlinkDialog(contact)
            else -> Unit
        }
    }

    private fun runWithUpdate(action: () -> Unit) {
        action()
//        refresh()
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
                dismissDialog.value = Unit
                showUnlinkSuccessDialog(contact)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
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
        modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = Navigation.ContactBookNavigation.BackToContactBook()
        }, modules))
    }
}

