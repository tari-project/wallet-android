package com.tari.android.wallet.ui.fragment.contact_book.link

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto

class ContactLinkViewModel : CommonViewModel() {

    val contact = MutableLiveData<ContactDto>()
    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)
    }

    fun initArgs(contact: ContactDto) {
        this.contact.value = contact
    }
}