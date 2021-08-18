package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

sealed class EnterRestorationPasswordNavigation {
    object ToRestoreInProgress : EnterRestorationPasswordNavigation()
}