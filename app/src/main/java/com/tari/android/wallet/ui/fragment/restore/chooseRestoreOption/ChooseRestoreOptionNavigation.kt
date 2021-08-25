package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

sealed class ChooseRestoreOptionNavigation {

    object ToEnterRestorePassword : ChooseRestoreOptionNavigation()

    object ToRestoreWithRecoveryPhrase : ChooseRestoreOptionNavigation()

    object ToRestoreInProgress : ChooseRestoreOptionNavigation()

    object OnRestoreCompleted : ChooseRestoreOptionNavigation()
}