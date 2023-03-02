package com.tari.android.wallet.ui.fragment.contact_book.details.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.DividerViewHolder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.SpaceVerticalViewHolder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile.ContactProfileViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.divider.SettingsDividerViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleView
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolder

class ContactDetailsAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(
        TitleViewHolder.getBuilder(),
        ContactProfileViewHolder.getBuilder(),
        SettingsDividerViewHolder.getBuilder(),
        ButtonViewHolder.getBuilder(),
        SettingsTitleViewHolder.getBuilder(),
        SpaceVerticalViewHolder.getBuilder(),
    )
}