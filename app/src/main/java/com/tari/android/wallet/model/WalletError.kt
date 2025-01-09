package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletError(
    override var code: Int,
    override var domain: String = "FFI",
) : CoreError(code, domain), Parcelable {

    constructor(ffiError: FFIError) : this(code = ffiError.code)

    constructor(ffiException: FFIException) : this(code = ffiException.error?.code ?: NoError.code)

    constructor(e: Throwable?) : this(
        code = when (e) {
            is FFIException -> e.error?.code ?: NoError.code
            is WalletException -> e.walletError.code
            else -> UnknownError.code
        },
    )

    companion object {
        val DatabaseDataError = WalletError(114)
        val TransactionNotFoundError = WalletError(204)
        val ContactNotFoundError = WalletError(401)
        val InvalidPassphraseEncryptionCypherError = WalletError(420)
        val InvalidPassphraseError = WalletError(428)
        val ValuesNotFound = WalletError(424)
        val SeedWordsInvalidDataError = WalletError(429)
        val SeedWordsVersionMismatchError = WalletError(430)
        val RecoveringWrongAddress = WalletError(702)
        val UnknownError = WalletError(-1)
        val NoError = WalletError(0)
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