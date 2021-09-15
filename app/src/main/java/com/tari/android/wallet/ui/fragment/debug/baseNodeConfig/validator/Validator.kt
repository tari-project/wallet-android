package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator

interface Validator {
    fun validate(string: String) : State

    sealed class State() {
        object Valid: State()
        object Invalid : State()
        object Neutral: State()
    }
}