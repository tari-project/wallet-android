package com.tari.android.wallet.ui.fragment.send.addRecepient

import com.tari.android.wallet.model.User

interface AddRecipientListener {

    fun continueToAmount(user: User)

}