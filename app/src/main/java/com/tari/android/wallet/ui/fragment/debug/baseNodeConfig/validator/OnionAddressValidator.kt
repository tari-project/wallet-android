package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator

class OnionAddressValidator: Validator {
    private val onion2AddressRegex = Regex("/onion/[a-zA-Z2-7]{16}(:[0-9]+)?")
    private val onion3AddressRegex = Regex("/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")

    override fun validate(string: String): Validator.State {
        return if (onion3AddressRegex.matches(string) || onion2AddressRegex.matches(string)) Validator.State.Valid else Validator.State.Invalid
    }
}