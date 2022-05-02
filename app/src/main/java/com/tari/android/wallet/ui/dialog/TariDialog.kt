package com.tari.android.wallet.ui.dialog

interface TariDialog {
    fun show()

    fun dismiss()

    fun isShowing(): Boolean
}