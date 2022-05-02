package com.tari.android.wallet.ui.fragment.settings.allSettings.button

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class ButtonViewDto(
    val title: String,
    val leftIconId: Int,
    val iconId: Int? = null,
    val style: ButtonStyle = ButtonStyle.Normal,
    val action: () -> Unit
) : CommonViewHolderItem()