package com.tari.android.wallet.ui.screen.restore.chooseRestoreOption

object ChooseRestoreOptionModel {
    data class UiState(
        val isStarted: Boolean = false,

        val paperWalletProgress: Boolean = false,
    )
}