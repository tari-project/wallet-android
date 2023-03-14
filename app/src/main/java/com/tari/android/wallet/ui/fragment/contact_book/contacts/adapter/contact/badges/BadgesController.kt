package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.badges

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.marginStart
import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction


class BadgesController(val view: ItemContactBinding) {

    private var isOpen = false
    var notifyAction: (ContactItem) -> Unit = { }
    private lateinit var contactItem: ContactItem

    private var lastAnimator: ValueAnimator? = null

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

        view.badgesContainer.removeAllViews()
        actions.forEach { contactAction ->
            BadgeItem(contactAction.icon) { performAction(contactAction) }.let { badgeItem ->
                view.badgesContainer.addView(BadgeItemView(view.root.context).apply { this.setItem(badgeItem) })
            }
        }
        view.badgesContainer.setVisible(false, View.INVISIBLE)
    }

    fun toggle() = process(!isOpen)

    fun process(newState: Boolean) {
        if (isOpen == newState) return

        isOpen = newState

        if (isOpen) {
            view.badgesContainer.setVisible(true, View.INVISIBLE)
            view.profileBadgesContainer.switch(true)
        }

        val startValue = if (isOpen) 0f else 1f
        val endValue = if (isOpen) 1f else 0f
        view.badgesContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val widthValue = view.badgesContainer.measuredWidth.toFloat()
        val profileWidth = view.profileContainer.width + view.profileContainer.marginStart * 2
        view.badgesContainer.setLayoutWidth(widthValue.toInt())

        if (!isOpen) {
            lastAnimator?.reverse()
        } else {
            lastAnimator = ValueAnimator.ofFloat(startValue, endValue).apply {
                addUpdateListener {
                    val value = it.animatedValue as Float
                    val width = (widthValue * value).toInt()
                    view.profileBadgesContainer.setLayoutWidth((profileWidth + width))
                }
                addListener(doOnEnd {
                    if (!isOpen) {
                        view.badgesContainer.setVisible(false, View.INVISIBLE)
                        view.profileBadgesContainer.switch(false)
                    }
                })
                duration = 400
                start()
            }
        }

        if (isOpen) notifyAction.invoke(contactItem)
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