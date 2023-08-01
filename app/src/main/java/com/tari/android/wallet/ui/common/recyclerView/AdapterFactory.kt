package com.tari.android.wallet.ui.common.recyclerView

object AdapterFactory {
    fun <T : CommonViewHolderItem> generate(vararg viewHolderBuilders: ViewHolderBuilder): CommonAdapter<T> {
        return object : CommonAdapter<T>() {
            override var viewHolderBuilders: List<ViewHolderBuilder> = viewHolderBuilders.toList()
        }
    }
}