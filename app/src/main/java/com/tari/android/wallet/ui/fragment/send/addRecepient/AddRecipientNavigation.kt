package com.tari.android.wallet.ui.fragment.send.addRecepient

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact

sealed class AddRecipientNavigation {
    class ToAmount(val tariContact: TariContact, val amount: MicroTari?) : AddRecipientNavigation()
}