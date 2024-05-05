package com.tari.android.wallet.ui.fragment.home.homeTransactionHistory

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.tx.TransactionRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import javax.inject.Inject

class HomeTransactionHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    var list = MediatorLiveData<List<CommonViewHolderItem>>()

    val searchBarVisible = MutableLiveData(false)
    val txEmptyStateVisible = MutableLiveData(false)
    val txListVisible = MutableLiveData(false)

    private val searchText = MutableLiveData("")
    private val searchEmptyStateVisible = MutableLiveData(false)

    init {
        component.inject(this)

        collectFlow(contactsRepository.contactList) {
            updateList()
        }

        list.addSource(transactionRepository.list) { updateList() }
        list.addSource(searchText) { updateList() }
    }

    private fun updateList() {
        val filtered = transactionRepository.list.value
        val searchText = searchText.value.orEmpty()
        val newList = (filtered.orEmpty().filter {
            searchText.isEmpty() || searchText.isNotEmpty() && (it is TransactionItem) && it.isContains(searchText)
        }.toMutableList())
        list.postValue(newList)

        val listIsEmpty = newList.isEmpty()
        val searchTextIsEmpty = searchText.isEmpty()

        searchBarVisible.postValue(!searchTextIsEmpty || !listIsEmpty)
        searchEmptyStateVisible.postValue(!searchTextIsEmpty && listIsEmpty)
        txEmptyStateVisible.postValue(searchTextIsEmpty && listIsEmpty)
        txListVisible.postValue(!listIsEmpty)
    }

    fun doSearch(text: String) {
        searchText.postValue(text)
    }
}