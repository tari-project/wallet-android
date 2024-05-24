package com.tari.android.wallet.model

import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException

open class WalletError(
    override var code: Int = NoError.code,
    override var domain: String = "FFI",
) : CoreError(code, domain) {

    object DatabaseDataError : WalletError(114)
    object TransactionNotFoundError : WalletError(204)
    object ContactNotFoundError : WalletError(401)
    object InvalidPassphraseEncryptionCypherError : WalletError(420)
    object InvalidPassphraseError : WalletError(428)
    object ValuesNotFound : WalletError(424)
    object SeedWordsInvalidDataError : WalletError(429)
    object SeedWordsVersionMismatchError : WalletError(430)
    object UnknownError : WalletError(-1)
    object NoError : WalletError(0)

    companion object {
        fun createFromFFI(error: FFIError): WalletError = WalletError(error.code)

        fun createFromFFI(error: FFIException): WalletError = WalletError(error.error?.code ?: NoError.code)

        fun createFromException(e: Throwable?): WalletError {
            if (e is FFIException) {
                return createFromFFI(e)
            } else if (e is WalletException) {
                return WalletError(e.walletError.code)
            }
            return UnknownError
        }
    }
}

fun throwIf(error: CoreError?) {
    if (error != null && error.code != 0) {
        throw WalletException(error)
    }
}

class WalletException(val walletError: CoreError) : RuntimeException(walletError.signature) {
    override fun toString(): String = "Unhandled wallet error: ${walletError.code}"
}