package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import java.math.BigInteger

/**
 * Pending outbound tx model class.
 *
 * @author The Tari Development Team
 */
class PendingOutboundTx() : Parcelable {

    var id = BigInteger("0")
    var destinationPublicKeyHexString = ""
    var amount = BigInteger("0")
    var timestamp = BigInteger("0")
    var message = ""

    constructor(
        id: BigInteger,
        destinationPublicKeyHexString: String,
        amount: BigInteger,
        timestamp: BigInteger,
        message: String
    ) : this() {
        this.id = id
        this.destinationPublicKeyHexString = destinationPublicKeyHexString
        this.amount = amount
        this.timestamp = timestamp
        this.message = message
    }

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<PendingOutboundTx> {

        override fun createFromParcel(parcel: Parcel): PendingOutboundTx {
            return PendingOutboundTx(parcel)
        }

        override fun newArray(size: Int): Array<PendingOutboundTx> {
            return Array(size) { PendingOutboundTx() }
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(id)
        parcel.writeString(destinationPublicKeyHexString)
        parcel.writeSerializable(amount)
        parcel.writeSerializable(timestamp)
        parcel.writeString(message)
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readSerializable() as BigInteger
        destinationPublicKeyHexString = inParcel.readString() ?: ""
        amount = inParcel.readSerializable() as BigInteger
        timestamp = inParcel.readSerializable() as BigInteger
        message = inParcel.readString() ?: ""
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion

}