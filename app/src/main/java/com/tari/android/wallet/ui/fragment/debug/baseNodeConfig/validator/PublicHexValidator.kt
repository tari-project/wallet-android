package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator

class PublicHexValidator: Validator {
    private val publicKeyRegex = Regex("[a-zA-Z0-9]{64}")

    override fun validate(string: String): Validator.State {
        return if (publicKeyRegex.matches(string)) Validator.State.Valid else Validator.State.Invalid
    }
}