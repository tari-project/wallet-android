package com.tari.android.wallet.ui.screen.restore.enterRestorationPassword

sealed class EnterRestorationPasswordState {

    object InitState : EnterRestorationPasswordState()

    object RestoringInProgressState : EnterRestorationPasswordState()

    object WrongPasswordErrorState : EnterRestorationPasswordState()
}

