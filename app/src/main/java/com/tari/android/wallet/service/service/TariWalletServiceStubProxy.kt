package com.tari.android.wallet.service.service

import com.tari.android.wallet.service.TariWalletService

class TariWalletServiceStubProxy : TariWalletService.Stub() {

    private var _stub: TariWalletServiceStubImpl? = null

    var stub: TariWalletServiceStubImpl?
        get() = _stub
        set(newStub) {
            _stub = newStub
        }
}