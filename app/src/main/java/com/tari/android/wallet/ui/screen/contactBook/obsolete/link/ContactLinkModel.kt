package com.tari.android.wallet.ui.screen.contactBook.obsolete.link

import com.tari.android.wallet.data.contacts.Contact

object ContactLinkModel {
    data class UiState(
        val contacts: List<Contact> = emptyList(),
        val searchQuery: String = "",
    )

    sealed class Effect {
        data object GrantPermission : Effect()
    }
}