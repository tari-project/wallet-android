package com.tari.android.wallet.ui.dialog

class ChangedPropertyDelegate<T>(initValue: T) {
    var value: T = initValue
        set(value) {
            val oldValue = field
            beforeTextChangeListener(oldValue, value)
            beforeTileChangeListener(oldValue, value)
            field = value
            afterTextChangeListener(oldValue, value)
            afterTileChangeListener(oldValue, value)
        }

    var beforeTextChangeListener: (oldValue: T, newValue: T) -> Unit = { _, _ -> }

    var afterTextChangeListener: (oldValue: T, newValue: T) -> Unit = { _, _ -> }

    var beforeTileChangeListener: (oldValue: T, newValue: T) -> Unit = { _, _ -> }

    var afterTileChangeListener: (oldValue: T, newValue: T) -> Unit = { _, _ -> }
}