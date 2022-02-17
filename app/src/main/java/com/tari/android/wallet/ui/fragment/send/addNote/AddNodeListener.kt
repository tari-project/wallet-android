package com.tari.android.wallet.ui.fragment.send.addNote

import com.tari.android.wallet.ui.fragment.send.common.TransactionData

interface AddNodeListener {

    fun continueToFinalizeSendTx(transactionData: TransactionData)
}