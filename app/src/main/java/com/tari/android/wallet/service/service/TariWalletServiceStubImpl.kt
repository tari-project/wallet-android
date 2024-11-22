package com.tari.android.wallet.service.service

import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService

class TariWalletServiceStubImpl(
    private val wallet: FFIWallet,
) : TariWalletService.Stub() {

    override fun getUnbindedOutputs(error: WalletError): List<TariUnblindedOutput> {
        return runMapping(error) {
            val ffiError = FFIError()
            val outputs = wallet.getUnbindedOutputs(ffiError)
            error.code = ffiError.code
            outputs
        }.orEmpty()
    }

    override fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: WalletError) {
        runMapping(error) {
            val ffiError = FFIError()
            wallet.restoreWithUnbindedOutputs(jsons, address, message, ffiError)
            error.code = ffiError.code
        }
    }

    private fun <T> runMapping(walletError: WalletError, onError: (Throwable) -> (Unit) = {}, action: () -> T?): T? {
        return try {
            action()
        } catch (throwable: Throwable) {
            onError(throwable)
            mapThrowableIntoError(walletError, throwable)
            null
        }
    }

    private fun mapThrowableIntoError(walletError: WalletError, throwable: Throwable) {
        if (throwable is FFIException) {
            if (throwable.error != null) {
                walletError.code = throwable.error.code
                return
            }
        }
        walletError.code = WalletError.UnknownError.code
    }
}