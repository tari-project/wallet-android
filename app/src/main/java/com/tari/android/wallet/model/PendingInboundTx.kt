package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import java.math.BigInteger

/**
 * Pending inbound tx model class.
 *
 * @author The Tari Development Team
 */
class PendingInboundTx() : Parcelable {

    var id = BigInteger("0")
    var sourcePublicKeyHexString = ""
    var amount = BigInteger("0")
    var timestamp = BigInteger("0")
    var message = ""

    constructor(
        id: BigInteger,
        sourcePublicKeyHexString: String,
        amount: BigInteger,
        timestamp: BigInteger,
        message: String
    ) : this() {
        this.id = id
        this.sourcePublicKeyHexString = sourcePublicKeyHexString
        this.amount = amount
        this.timestamp = timestamp
        this.message = message
    }

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<PendingInboundTx> {

        override fun createFromParcel(parcel: Parcel): PendingInboundTx {
            return PendingInboundTx(parcel)
        }

        override fun newArray(size: Int): Array<PendingInboundTx> {
            return Array(size) { PendingInboundTx() }
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(id)
        parcel.writeString(sourcePublicKeyHexString)
        parcel.writeSerializable(amount)
        parcel.writeSerializable(timestamp)
        parcel.writeString(message)
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readSerializable() as BigInteger
        sourcePublicKeyHexString = inParcel.readString() ?: ""
        amount = inParcel.readSerializable() as BigInteger
        timestamp = inParcel.readSerializable() as BigInteger
        message = inParcel.readString() ?: ""
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion

}