package com.tari.android.wallet.ffi

import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.tx.Tx

abstract class FFITxBase() : FFIBase() {

    constructor(pointer: FFIPointer) : this() {
        if (pointer.isNull()) error("Pointer must not be null")
        this.pointer = pointer
    }

    abstract fun getSourcePublicKey(): FFITariWalletAddress
    abstract fun getDestinationPublicKey(): FFITariWalletAddress
    abstract fun isOutbound(): Boolean

    fun getContact(): TariContact {
        val publicKey = if (isOutbound()) {
            getDestinationPublicKey().runWithDestroy {
                TariWalletAddress(it)
            }
        } else {
            getSourcePublicKey().runWithDestroy {
                TariWalletAddress(it)
            }
        }
        return TariContact(publicKey)
    }

    fun getDirection(): Tx.Direction = if (isOutbound()) Tx.Direction.OUTBOUND else Tx.Direction.INBOUND
}