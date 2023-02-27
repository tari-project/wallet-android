package com.tari.android.wallet.ui.fragment.contact_book.root

import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto

sealed class ContactBookNavigation {

    class ToContactDetails(val contact: ContactDto) : ContactBookNavigation()

    class ToAddContact : ContactBookNavigation()

    class ToSendTari(val contact: ContactDto) : ContactBookNavigation()

    class ToRequestTari(val contact: ContactDto) : ContactBookNavigation()

    class ToExternalWallet(val contact: ContactDto) : ContactBookNavigation()
}

