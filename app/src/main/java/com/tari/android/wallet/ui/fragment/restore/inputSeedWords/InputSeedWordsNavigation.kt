package com.tari.android.wallet.ui.fragment.restore.inputSeedWords

sealed class InputSeedWordsNavigation {
    object ToRestoreFormSeedWordsInProgress : InputSeedWordsNavigation()
    object ToBaseNodeSelection: InputSeedWordsNavigation()
}