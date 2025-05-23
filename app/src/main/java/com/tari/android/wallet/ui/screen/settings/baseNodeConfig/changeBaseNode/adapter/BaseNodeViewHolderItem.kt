package com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.adapter

import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class BaseNodeViewHolderItem(val baseNodeDto: BaseNodeDto, val currentBaseNode: BaseNodeDto?, val deleteAction: (BaseNodeDto) -> Unit) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "BaseNodeViewHolderItem" + baseNodeDto.address
}