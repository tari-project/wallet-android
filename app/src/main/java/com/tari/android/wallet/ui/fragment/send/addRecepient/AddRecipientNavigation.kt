package com.tari.android.wallet.ui.fragment.send.addRecepient

import com.tari.android.wallet.model.User

sealed class AddRecipientNavigation {
    class ToAmount(val user: User) : AddRecipientNavigation()
}