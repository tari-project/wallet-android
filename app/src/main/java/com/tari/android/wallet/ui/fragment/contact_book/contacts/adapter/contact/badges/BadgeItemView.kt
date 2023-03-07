package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.badges

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.ViewBadgeItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible

class BadgeItemView : CommonView<CommonViewModel, ViewBadgeItemBinding> {
    constructor(context: Context) : super(context, null) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    override fun setup() = Unit
    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewBadgeItemBinding =
        ViewBadgeItemBinding.inflate(layoutInflater, parent, attachToRoot)

    private fun init() = Unit

    fun setItem(item: BadgeItem) {
        ui.badgeItemIconImageView.setImageResource(item.icon)
        ui.root.setOnClickListener { item.action.invoke() }
    }
}

