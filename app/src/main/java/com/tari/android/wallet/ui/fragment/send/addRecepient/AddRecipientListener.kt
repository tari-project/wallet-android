package com.tari.android.wallet.ui.fragment.send.addRecepient

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

interface AddRecipientListener {

    fun continueToAmount(user: ContactDto, amount: MicroTari?)

}