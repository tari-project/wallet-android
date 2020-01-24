package com.tari.android.wallet.model

import java.math.BigInteger

abstract class Tx {

    enum class Direction {
        INBOUND,
        OUTBOUND
    }

    var id = BigInteger("0")
    var direction = Direction.INBOUND
    var amount = MicroTari(BigInteger("0"))
    var timestamp = BigInteger("0")
    var message = ""
    /**
     * This is the receiver's alias for outbound txs and sender's alias for inbound txs.
     */
    var contact = Contact()

}