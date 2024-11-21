package com.tari.android.wallet.service.service

import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariVector
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService

class TariWalletServiceStubProxy : TariWalletService.Stub() {

    private var _stub: TariWalletServiceStubImpl? = null

    var stub: TariWalletServiceStubImpl?
        get() = _stub
        set(newStub) {
            _stub = newStub
        }

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? = stub?.getUtxos(page, pageSize, sorting, error)

    override fun getAllUtxos(error: WalletError): TariVector? = stub?.getAllUtxos(error)

    override fun previewJoinUtxos(utxos: List<TariUtxo>, error: WalletError): TariCoinPreview? = stub?.previewJoinUtxos(utxos, error)

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError): TariCoinPreview? =
        stub?.previewSplitUtxos(utxos, splitCount, error)

    override fun joinUtxos(utxos: List<TariUtxo>, error: WalletError) = stub?.joinUtxos(utxos, error) ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError) = stub?.splitUtxos(utxos, splitCount, error) ?: Unit

    override fun getUnbindedOutputs(error: WalletError): List<TariUnblindedOutput> = stub?.getUnbindedOutputs(error) ?: listOf()

    override fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: WalletError) =
        stub?.restoreWithUnbindedOutputs(jsons, address, message, error) ?: Unit
}