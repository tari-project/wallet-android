package com.tari.android.wallet.ui.fragment.settings.allSettings.button

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class ButtonViewDto(val title: String, val iconId: Int? = null, val action: () -> Unit) : CommonViewHolderItem()