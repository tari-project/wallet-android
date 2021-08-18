package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

sealed class ChooseRestoreOptionState {
    object BeginProgress : ChooseRestoreOptionState()

    object EndProgress : ChooseRestoreOptionState()
}