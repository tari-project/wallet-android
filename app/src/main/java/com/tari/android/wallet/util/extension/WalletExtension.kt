package com.tari.android.wallet.util.extension

import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.throwIf

fun <T> FFIWallet.getWithError(
    onError: (error: WalletError) -> Unit = { throwIf(it) },
    action: (wallet: FFIWallet) -> T,
): T? = try {
    action(this)
} catch (throwable: Throwable) {
    onError(throwable.mapIntoError())
    null
}

fun <T> FFIWallet.getOrNull(
    action: (wallet: FFIWallet) -> T,
): T? = try {
    action(this)
} catch (throwable: Throwable) {
    null
}

private fun Throwable.mapIntoError(): WalletError {
    return if (this is FFIException && this.error != null) {
        WalletError(this.error.code)
    } else {
        WalletError(WalletError.UnknownError.code)
    }
}