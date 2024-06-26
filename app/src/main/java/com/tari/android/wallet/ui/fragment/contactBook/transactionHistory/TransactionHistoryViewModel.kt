package com.tari.android.wallet.ui.fragment.contactBook.transactionHistory

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.tx.TransactionRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import javax.inject.Inject

class TransactionHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    var selectedContact = MutableLiveData<ContactDto>()

    var list = MediatorLiveData<List<CommonViewHolderItem>>()

    init {
        component.inject(this)

        collectFlow(contactsRepository.contactList) {
            updateList()
        }

        list.addSource(selectedContact) { updateList() }
        list.addSource(transactionRepository.list) { updateList() }
    }

    private fun updateList() {
        val contact = selectedContact.value ?: return
        val actualContact = contactsRepository.getByUuid(contact.uuid)
        if (contact != actualContact) selectedContact.postValue(actualContact)

        val filtered = transactionRepository.list.value?.filter {
            if (it is TransactionItem) {
                it.tx.tariContact.walletAddress == selectedContact.value?.contactInfo?.extractWalletAddress()
            } else {
                false
            }
        }.orEmpty().map { (it as TransactionItem).copy(contact = actualContact) }
        list.postValue(filtered)
    }
}

