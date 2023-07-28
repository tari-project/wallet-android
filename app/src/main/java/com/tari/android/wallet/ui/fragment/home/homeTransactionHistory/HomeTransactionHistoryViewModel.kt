package com.tari.android.wallet.ui.fragment.home.homeTransactionHistory

import androidx.lifecycle.MediatorLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.tx.TransactionRepository
import javax.inject.Inject

class HomeTransactionHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    var list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)

        list.addSource(transactionRepository.list) { updateList() }
    }

    private fun updateList() {
        val filtered = transactionRepository.list.value
        list.postValue(filtered.orEmpty().toMutableList())
    }
}