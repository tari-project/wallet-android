package com.tari.android.wallet.service.service

import com.tari.android.wallet.model.TariUnblindedOutput
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

    override fun getUnbindedOutputs(error: WalletError): List<TariUnblindedOutput> = stub?.getUnbindedOutputs(error) ?: listOf()

    override fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: WalletError) =
        stub?.restoreWithUnbindedOutputs(jsons, address, message, error) ?: Unit
}