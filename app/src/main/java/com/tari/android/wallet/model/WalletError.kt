package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException
import kotlinx.parcelize.Parcelize

@Parcelize
open class WalletError(
    override var code: Int = NoError.code,
    override var domain: String = "FFI",
) : CoreError(code, domain), Parcelable {

    companion object {
        val DatabaseDataError = WalletError(114)
        val TransactionNotFoundError = WalletError(204)
        val ContactNotFoundError = WalletError(401)
        val InvalidPassphraseEncryptionCypherError = WalletError(420)
        val InvalidPassphraseError = WalletError(428)
        val ValuesNotFound = WalletError(424)
        val SeedWordsInvalidDataError = WalletError(429)
        val SeedWordsVersionMismatchError = WalletError(430)
        val UnknownError = WalletError(-1)
        val NoError = WalletError(0)

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