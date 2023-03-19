package com.tari.android.wallet.ui.fragment.contact_book.link

import android.text.SpannableString
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.tari.android.wallet.R.string.common_cancel
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.common_confirm
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_success_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_link_title
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_title
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import io.reactivex.BackpressureStrategy
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactLinkViewModel : CommonViewModel() {

    val contactListSource = MediatorLiveData<List<ContactItem>>()

    val searchText = MutableLiveData("")

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val ffiContact = MutableLiveData<ContactDto>()

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)

        contactListSource.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) {
            contactListSource.value = it.map { contactDto -> ContactItem(contactDto, true) }
        }

        list.addSource(contactListSource) { updateList() }
        list.addSource(searchText) { updateList() }
    }

    fun initArgs(contact: ContactDto) {
        this.ffiContact.value = contact
    }

    fun onContactClick(item: CommonViewHolderItem) {
        showLinkDialog((item as ContactItem).contact)
    }

    fun onSearchQueryChanged(query: String) {
        searchText.value = query
    }

    private fun updateList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        var list = source.filter { it.contact.contact is PhoneContactDto }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        list = list.sortedBy { it.contact.contact.getAlias().lowercase() }

        this.list.postValue(list.toMutableList())
    }

    private fun showLinkDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.value!!.contact.extractWalletAddress()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_link_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_confirm), ButtonStyle.Normal) {
                contactsRepository.linkContacts(ffiContact.value!!, phoneContactDto)
                _dismissDialog.postValue(Unit)
                showLinkSuccessDialog(phoneContactDto)
            },
            ButtonModule(resourceManager.getString(common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showLinkSuccessDialog(phoneContactDto: ContactDto) {
        val tariWalletAddress = ffiContact.value!!.contact.extractWalletAddress()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_link_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(tariWalletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_close), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = Navigation.ContactBookNavigation.BackToContactBook()
        }, modules))
    }
}