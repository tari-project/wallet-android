package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction

class BadgesController(val view: ItemContactBinding) {

    var isOpen = false
    var notifyAction: (ContactItem) -> Unit = { }
    lateinit var contactItem: ContactItem

    init {
        view.profileContainer.setOnClickListener { toggle() }
        view.profileBadgesContainer.updateBack(backColor = PaletteManager().getPurpleBrand(view.root.context))
        view.profileBadgesContainer.switch(false)
    }

    fun bind(item: ContactItem) {
        contactItem = item
    }

    fun toggle() = process(!isOpen)

    fun process(newState: Boolean) {
        if (isOpen == newState) return
        val oldState = isOpen
        isOpen = newState

        view.profileBadgesContainer.switch(newState)

        //todo


        if (isOpen) notifyAction.invoke(contactItem)
    }

    companion object {
        val availableContactActions = listOf(
            ContactAction.Send,
            ContactAction.ToFavorite,
            ContactAction.ToUnFavorite,
            ContactAction.OpenProfile,
            ContactAction.Link,
            ContactAction.Unlink
        )
    }
}