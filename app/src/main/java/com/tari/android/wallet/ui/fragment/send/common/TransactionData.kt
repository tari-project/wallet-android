package com.tari.android.wallet.ui.fragment.send.common

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import java.io.Serializable

data class TransactionData(val recipientUser: User?, val amount: MicroTari?, val note: String?) : Serializable