package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariUtxo
import java.math.BigInteger

class TariUtxo() : Parcelable {

    var commitment: String = ""
    var value: MicroTari = MicroTari(BigInteger.ZERO)
    var minedHeight: Long = -1

    constructor(parcel: Parcel) : this() {
        commitment = parcel.readString()!!
        value = parcel.readParcelable(MicroTari::class.java.classLoader)!!
        minedHeight = parcel.readLong()
    }

    constructor(ffiUtxo: FFITariUtxo) : this() {
        commitment = ffiUtxo.commitment
        value = MicroTari(BigInteger.valueOf(ffiUtxo.value))
        minedHeight = ffiUtxo.minedHeight
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(commitment)
        parcel.writeParcelable(value, flags)
        parcel.writeLong(minedHeight)
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


}