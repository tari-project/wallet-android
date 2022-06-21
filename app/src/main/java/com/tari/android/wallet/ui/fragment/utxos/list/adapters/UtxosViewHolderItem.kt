package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import org.joda.time.DateTime

class UtxosViewHolderItem(val amount: String, val hash: String, val checked: Boolean, val status: String, val dateTime: DateTime) :
    CommonViewHolderItem()