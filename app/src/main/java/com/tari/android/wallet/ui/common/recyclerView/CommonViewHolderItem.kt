package com.tari.android.wallet.ui.common.recyclerView

import java.io.Serializable

abstract class CommonViewHolderItem : Serializable {

    var rebindAction: () -> Unit = {}

    fun rebind() {
        runCatching {
            rebindAction()
        }
    }
}