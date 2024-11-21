package com.tari.android.wallet.ui.fragment.contactBook.link

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

object ContactLinkModel {
    data class UiState(
        val contacts: List<ContactDto> = emptyList(),
        val searchQuery: String = "",
    )

    sealed class Effect {
        data object GrantPermission : Effect()
    }
}