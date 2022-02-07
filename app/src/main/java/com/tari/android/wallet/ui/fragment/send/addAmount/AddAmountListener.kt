package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User

interface AddAmountListener {

    fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment)

    fun continueToAddNote(recipientUser: User, amount: MicroTari, isOneSidePayment: Boolean)
}