package com.tari.android.wallet.ui.component.mainList

import androidx.activity.OnBackPressedCallback

class MutedBackPressedCallback(enabled: Boolean = true) : OnBackPressedCallback(enabled) {
    override fun handleOnBackPressed() = Unit
}