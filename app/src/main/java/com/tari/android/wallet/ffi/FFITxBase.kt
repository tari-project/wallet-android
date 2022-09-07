package com.tari.android.wallet.ffi

import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User

abstract class FFITxBase() : FFIBase() {

    constructor(pointer: FFIPointer): this() {
        this.pointer = pointer
    }

    abstract fun getSourcePublicKey() : FFIPublicKey
    abstract fun getDestinationPublicKey() : FFIPublicKey
    abstract fun isOutbound() : Boolean

    fun getUser(): User {
        val publicKey: PublicKey
        if (isOutbound()) {
            val destination = getDestinationPublicKey()
            val destinationHex = destination.toString()
            val destinationEmoji = destination.getEmojiId()
            publicKey = PublicKey(destinationHex, destinationEmoji)
            destination.destroy()
        } else {
            val source = getSourcePublicKey()
            val sourceHex = source.toString()
            val sourceEmoji = source.getEmojiId()
            publicKey = PublicKey(sourceHex, sourceEmoji)
            source.destroy()
        }
        return User(publicKey)
    }

    fun getDirection() : Tx.Direction = if (isOutbound()) Tx.Direction.OUTBOUND else Tx.Direction.INBOUND
}