package com.tari.android.wallet.ui.fragment.utxos.list.module

import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.Ordering

class ListItemModule(val ordering: Ordering, var click: () -> Unit = {}) : IDialogModule() {
    var listener: (Boolean) -> Unit = {}

    var isSelected: Boolean = false
        set(value) {
            listener.invoke(value)
            field = value
        }
}

