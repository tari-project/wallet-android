package com.tari.android.wallet.ui.fragment.contact_book.transactionHistory

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.tx.TransactionRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import io.reactivex.BackpressureStrategy
import javax.inject.Inject

class TransactionHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var contactRepository: ContactsRepository

    var selectedContact = MutableLiveData<ContactDto>()

    var list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)

        list.addSource(contactRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) {
            updateList()
        }

        list.addSource(selectedContact) { updateList() }

        list.addSource(transactionRepository.list) { updateList() }
    }

    private fun updateList() {
        val contact = selectedContact.value ?: return
        val actualContact = contactRepository.getByUuid(contact.uuid)
        if (contact != actualContact) selectedContact.postValue(actualContact)

        val filtered: MutableList<CommonViewHolderItem> = transactionRepository.list.value?.filter {
            if (it is TransactionItem) {
                it.tx.tariContact.walletAddress == selectedContact.value?.contact?.extractWalletAddress()
            } else {
                false
            }
        }.orEmpty().map { (it as TransactionItem).copy(contact = actualContact) }.toMutableList()
        list.postValue(filtered)
    }
}

