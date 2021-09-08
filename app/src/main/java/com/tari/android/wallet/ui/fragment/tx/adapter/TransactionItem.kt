package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.tx.gif.GIFViewModel

internal class TransactionItem(val tx: Tx, val position: Int, val viewModel: GIFViewModel, val requiredConfirmationCount: Long) :
    CommonViewHolderItem()