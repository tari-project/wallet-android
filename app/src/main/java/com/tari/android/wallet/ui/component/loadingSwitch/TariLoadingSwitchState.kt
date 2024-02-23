package com.tari.android.wallet.ui.component.loadingSwitch

data class TariLoadingSwitchState(
    val isChecked: Boolean = false,
    val isLoading: Boolean = false,
) {
    fun startLoading() = this.copy(isLoading = true)
    fun stopLoading() = this.copy(isLoading = false)
}