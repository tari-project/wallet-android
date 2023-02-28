package com.tari.android.wallet.ui.fragment.contact_book.root

import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact

sealed class ContactBookNavigation {

    class ToContactDetails(val contact: ContactDto) : ContactBookNavigation()

    object ToAddContact : ContactBookNavigation()

    class ToAddContactName(val contact: IContact) : ContactBookNavigation()

    object ToFinalizeAddingContact : ContactBookNavigation()

    class ToSendTari(val contact: ContactDto) : ContactBookNavigation()

    class ToRequestTari(val contact: ContactDto) : ContactBookNavigation()

    class ToExternalWallet(val contact: ContactDto) : ContactBookNavigation()
}

