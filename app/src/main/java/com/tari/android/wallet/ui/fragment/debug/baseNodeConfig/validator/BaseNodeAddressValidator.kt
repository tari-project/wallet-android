package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator

class BaseNodeAddressValidator: Validator {
    private val onion2ClipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion/[a-zA-Z2-7]{16}(:[0-9]+)?")
    private val onion3ClipboardRegex = Regex("[a-zA-Z0-9]{64}::/onion[2-3]/[a-zA-Z2-7]{56}(:[0-9]+)?")

    override fun validate(string: String): Validator.State {
        return if (onion2ClipboardRegex.matches(string) || onion3ClipboardRegex.matches(string)) Validator.State.Valid else Validator.State.Invalid
    }
}