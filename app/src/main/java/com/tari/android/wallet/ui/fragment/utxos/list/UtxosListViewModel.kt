package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderDirection
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderType

class UtxosListViewModel : CommonViewModel() {

    val listType: MutableLiveData<ListType> = MutableLiveData()

    fun setTypeList(listType: ListType) {
        this.listType.postValue(listType)
    }

    fun setOrderingDirection(ordering: OrderDirection) {

    }

    fun setOrderingType(type: OrderType) {

    }

    fun setSelectionState(isSelecting: Boolean) {

    }
}