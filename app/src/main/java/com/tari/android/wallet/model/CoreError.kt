package com.tari.android.wallet.model

open class CoreError(
    open var code: Int,
    open var domain: String,
) {
    val signature: String
        get() = "$domain-$code"

    override fun equals(other: Any?): Boolean = (other as? CoreError)?.code == code

    override fun hashCode(): Int = code.hashCode()
}


