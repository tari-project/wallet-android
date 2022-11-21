package com.tari.android.wallet.ffi

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User

abstract class FFITxBase() : FFIBase() {

    constructor(pointer: FFIPointer) : this() {
        this.pointer = pointer
    }

    abstract fun getSourcePublicKey(): FFITariWalletAddress
    abstract fun getDestinationPublicKey(): FFITariWalletAddress
    abstract fun isOutbound(): Boolean

    fun getUser(): User {
        val publicKey = if (isOutbound()) {
            getDestinationPublicKey().runWithDestroy {
                val destinationHex = it.toString()
                val destinationEmoji = it.getEmojiId()
                TariWalletAddress(destinationHex, destinationEmoji)
            }
        } else {
            getSourcePublicKey().runWithDestroy {
                val sourceHex = it.toString()
                val sourceEmoji = it.getEmojiId()
                TariWalletAddress(sourceHex, sourceEmoji)
            }
        }
        return User(publicKey)
    }

    fun getDirection(): Tx.Direction = if (isOutbound()) Tx.Direction.OUTBOUND else Tx.Direction.INBOUND
}