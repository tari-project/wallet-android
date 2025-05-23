package com.tari.android.wallet.ui.screen.settings.logs.logs.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class LogViewHolderItem(val log: DebugLog) : CommonViewHolderItem() {
    override val viewHolderUUID: String = log.toString()
}