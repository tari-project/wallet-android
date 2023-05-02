package com.tari.android.wallet.ui.fragment.contact_book.transactionHistory

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.tx.TransactionRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import javax.inject.Inject

class TransactionHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    var selectedContact = MutableLiveData<ContactDto>()

    var list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)

        list.addSource(selectedContact) { updateList() }

        list.addSource(transactionRepository.debouncedList) { updateList() }
    }

    private fun updateList() {
        val filtered = transactionRepository.list.value?.filter {
            if (it is TransactionItem) {
                it.tx.tariContact.walletAddress == selectedContact.value?.contact?.extractWalletAddress()
            } else {
                false
            }
        }
        list.postValue(filtered.orEmpty().toMutableList())
    }
}