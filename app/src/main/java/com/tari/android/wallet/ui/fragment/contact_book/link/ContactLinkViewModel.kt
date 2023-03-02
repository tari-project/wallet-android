package com.tari.android.wallet.ui.fragment.contact_book.link

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import javax.inject.Inject

class ContactLinkViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val contact = MutableLiveData<ContactDto>()
    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)
    }

    fun initArgs(contact: ContactDto) {
        this.contact.value = contact
    }

    private fun showUnlinkDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_contacts_book_link_message2)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                contactsRepository.unlinkContact(contact.value!!)
                _dismissDialog.postValue(Unit)
                showUnlinkSuccessDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showUnlinkSuccessDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_contacts_book_link_success_message)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) { _backPressed.postValue(Unit) },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs { _backPressed.postValue(Unit) }, modules))
    }
}