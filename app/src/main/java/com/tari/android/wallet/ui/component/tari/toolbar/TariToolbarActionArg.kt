package com.tari.android.wallet.ui.component.tari.toolbar

class TariToolbarActionArg(
    val icon: Int? = null,
    val drawable: Int? = null,
    val title: String? = null,
    val isBack: Boolean = false,
    val action: (() -> Unit)? = null
)