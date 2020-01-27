package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import java.math.BigInteger

/**
 * Pending outbound tx model class.
 *
 * @author The Tari Development Team
 */
class PendingOutboundTx() : Tx(), Parcelable {

    constructor(
        id: BigInteger,
        contact: Contact,
        amount: MicroTari,
        timestamp: BigInteger,
        message: String
    ) : this() {
        this.id = id
        this.direction = Direction.OUTBOUND
        this.contact = contact
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
        parcel.writeSerializable(direction)
        parcel.writeParcelable(contact, flags)
        parcel.writeParcelable(amount, flags)
        parcel.writeSerializable(timestamp)
        parcel.writeString(message)
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readSerializable() as BigInteger
        direction = inParcel.readSerializable() as Direction
        contact = inParcel.readParcelable(Contact::class.java.classLoader)!!
        amount = inParcel.readParcelable(MicroTari::class.java.classLoader)!!
        timestamp = inParcel.readSerializable() as BigInteger
        message = inParcel.readString() ?: ""
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion

}