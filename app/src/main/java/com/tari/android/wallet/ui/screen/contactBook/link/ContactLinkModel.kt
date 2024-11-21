package com.tari.android.wallet.ui.screen.contactBook.link

import com.tari.android.wallet.data.contacts.model.ContactDto

object ContactLinkModel {
    data class UiState(
        val contacts: List<ContactDto> = emptyList(),
        val searchQuery: String = "",
    )

    sealed class Effect {
        data object GrantPermission : Effect()
    }
}