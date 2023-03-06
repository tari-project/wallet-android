package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.badges

import android.view.View
import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction


class BadgesController(val view: ItemContactBinding) {

    private var isOpen = false
    var notifyAction: (ContactItem) -> Unit = { }
    private lateinit var contactItem: ContactItem

    private val actions = mutableListOf<ContactAction>()

    init {
        view.profileBadgesContainer.updateBack(backColor = PaletteManager().getPurpleBrand(view.root.context))
        view.profileBadgesContainer.switch(false)
        view.profileBadgesContainerInner.outlineProvider = view.profileBadgesContainer.outlineProvider
    }

    fun bind(item: ContactItem) {
        contactItem = item
        isOpen = false

        if (item.isSimple) {
            view.profileContainer.setOnClickListener { }
        } else {
            view.profileContainer.setOnClickListener { toggle() }
        }

        actions.clear()
        val contactActions = item.contact.getContactActions()
        actions.addAll(availableContactActions.filter { contactAction -> contactActions.contains(contactAction) })

        view.profileBadgesContainerInner.removeViews(1, view.profileBadgesContainerInner.childCount - 1)
        actions.forEach { contactAction ->
            BadgeItem(contactAction.icon) { performAction(contactAction) }.let { badgeItem ->
                view.profileBadgesContainerInner.addView(BadgeItemView(view.root.context).apply { this.setItem(badgeItem) })
            }
        }
        setVisibleItems()
    }

    fun toggle() = process(!isOpen)

    fun process(newState: Boolean) {
        if (isOpen == newState) return
        val oldState = isOpen
        isOpen = newState

        view.profileBadgesContainer.switch(newState)

        setVisibleItems()

        if (isOpen) notifyAction.invoke(contactItem)
    }

    private fun setVisibleItems() {
        (1 until view.profileBadgesContainerInner.childCount)
            .map { view.profileBadgesContainerInner.getChildAt(it) }
            .forEach { toggle(it, isOpen) }
    }

    private fun toggle(view: View, isVisible: Boolean) {
        view.setVisible(isVisible)
    }

    private fun performAction(contactAction: ContactAction) {
        toggle()
        contactItem.contactAction(contactItem.contact, contactAction)
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