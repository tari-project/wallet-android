package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.ui.fragment.send.common.TransactionData

interface AddAmountListener {

    fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment)

    fun continueToAddNote(transactionData: TransactionData)

    fun continueToFinalizing(transactionData: TransactionData)
}