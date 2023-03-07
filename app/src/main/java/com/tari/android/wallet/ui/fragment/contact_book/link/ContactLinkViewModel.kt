package com.tari.android.wallet.ui.fragment.contact_book.link

import android.text.SpannableString
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.toLiveData
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import io.reactivex.BackpressureStrategy
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactLinkViewModel : CommonViewModel() {

    val contactListSource = MediatorLiveData<List<ContactItem>>()

    val searchText = MutableLiveData("")

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val navigation = SingleLiveEvent<ContactBookNavigation>()

    val ffiContact = MutableLiveData<ContactDto>()

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)

        contactListSource.addSource(contactsRepository.publishSubject.toLiveData(BackpressureStrategy.LATEST)) {
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

    private fun updateList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        var list = source.filter { it.contact.isDeleted.not() }.filter { it.contact.contact is PhoneContactDto }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        this.list.postValue(list.toMutableList())
    }

    private fun showLinkDialog(phoneContactDto: ContactDto) {
        val shortEmoji = ffiContact.value!!.contact.extractWalletAddress().extractShortVersion()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val bodyHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_link_message2, shortEmoji, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_link_title)),
            BodyModule(null, SpannableString(bodyHtml)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                contactsRepository.linkContacts(ffiContact.value!!, phoneContactDto)
                _dismissDialog.postValue(Unit)
                showLinkSuccessDialog(phoneContactDto)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showLinkSuccessDialog(phoneContactDto: ContactDto) {
        val shortEmoji = ffiContact.value!!.contact.extractWalletAddress().extractShortVersion()
        val name = (phoneContactDto.contact as PhoneContactDto).firstName
        val bodyHtml =
            HtmlHelper.getSpannedText(resourceManager.getString(R.string.contact_book_contacts_book_link_success_message, shortEmoji, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(bodyHtml)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = ContactBookNavigation.BackToContactBook()
        }, modules))
    }
}