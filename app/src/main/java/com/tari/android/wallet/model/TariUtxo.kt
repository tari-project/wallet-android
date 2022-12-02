package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariUtxo
import com.tari.android.wallet.ui.extension.readP
import java.math.BigInteger

class TariUtxo() : Parcelable {

    var commitment: String = ""
    var value: MicroTari = MicroTari(BigInteger.ZERO)
    var minedHeight: Long = -1
    var timestamp: Long = -1
    var status: UtxoStatus = UtxoStatus.Spent

    constructor(parcel: Parcel) : this() {
        commitment = parcel.readString()!!
        value = parcel.readP(MicroTari::class.java)
        minedHeight = parcel.readLong()
        timestamp = parcel.readLong()
        status = UtxoStatus.fromValue(parcel.readInt())
    }

    constructor(ffiUtxo: FFITariUtxo) : this() {
        commitment = ffiUtxo.commitment
        value = MicroTari(BigInteger.valueOf(ffiUtxo.value))
        minedHeight = ffiUtxo.minedHeight
        timestamp = ffiUtxo.minedTimestamp
        status = UtxoStatus.fromValue(ffiUtxo.status.toInt())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(commitment)
        parcel.writeParcelable(value, flags)
        parcel.writeLong(minedHeight)
        parcel.writeLong(timestamp)
        parcel.writeInt(status.value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TariUtxo> {
        override fun createFromParcel(parcel: Parcel): TariUtxo {
            return TariUtxo(parcel)
        }

        override fun newArray(size: Int): Array<TariUtxo?> {
            return arrayOfNulls(size)
        }
    }

    enum class UtxoStatus(val value: Int) {
        Unspent(0),
        Spent(1),
        EncumberedToBeReceived(2),
        EncumberedToBeSpent(3),
        Invalid(4),
        CancelledInbound(5),
        UnspentMinedUnconfirmed(6),
        ShortTermEncumberedToBeReceived(7),
        ShortTermEncumberedToBeSpent(8),
        SpentMinedUnconfirmed(9),
        AbandonedCoinbase(10),
        NotStored(11);

        companion object {
            fun fromValue(value: Int): UtxoStatus = values().first { it.value == value }
        }
    }
}