package com.tari.android.wallet.ui.fragment.contact_book.root

import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

sealed class ContactBookNavigation {

    class ToContactDetails(val contact: ContactDto) : ContactBookNavigation()

    object ToAddContact : ContactBookNavigation()

    class ToAddContactName(val contact: ContactDto) : ContactBookNavigation()

    class ToSendTari(val contact: ContactDto) : ContactBookNavigation()

    class ToRequestTari(val contact: ContactDto) : ContactBookNavigation()

    class ToExternalWallet(val contact: ContactDto) : ContactBookNavigation()

    class ToLinkContact(val contact: ContactDto) : ContactBookNavigation()

    class BackToContactBook() : ContactBookNavigation()
}

