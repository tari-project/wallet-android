package com.tari.android.wallet.service.service

import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariVector
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.util.Constants

class TariWalletServiceStubImpl(
    private val wallet: FFIWallet,
) : TariWalletService.Stub() {

    override fun getSeedWords(error: WalletError): List<String>? = runMapping(error) {
        wallet.getSeedWords().runWithDestroy { seedWords -> (0 until seedWords.getLength()).map { seedWords.getAt(it) } }
    }

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? =
        runMapping(error) { wallet.getUtxos(page, pageSize, sorting) }

    override fun getAllUtxos(error: WalletError): TariVector? = runMapping(error) { wallet.getAllUtxos() }

    override fun joinUtxos(utxos: List<TariUtxo>, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.joinUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.splitUtxos(utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun previewJoinUtxos(utxos: List<TariUtxo>, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.joinPreviewUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
        result
    }

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.splitPreviewUtxos(
            utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError
        )
        walletError.code = ffiError.code
        result
    }

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

    private fun mapThrowableIntoError(walletError: WalletError, throwable: Throwable) {
        if (throwable is FFIException) {
            if (throwable.error != null) {
                walletError.code = throwable.error.code
                return
            }
        }
        walletError.code = WalletError.UnknownError.code
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
}