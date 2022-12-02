package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariCoinPreview
import com.tari.android.wallet.ffi.FFITariVector
import com.tari.android.wallet.ui.extension.readP
import java.math.BigInteger

class TariCoinPreview() : Parcelable {

    var vector: TariVector = TariVector()
    var feeValue: MicroTari = MicroTari(BigInteger.ZERO)

    constructor(parcel: Parcel) : this() {
        vector = parcel.readP(TariVector::class.java)
        feeValue = parcel.readP(MicroTari::class.java)
    }

    constructor(ffiTariCoinPreview: FFITariCoinPreview) : this() {
        vector = TariVector(FFITariVector(ffiTariCoinPreview.vectorPointer))
        feeValue = MicroTari(BigInteger.valueOf(ffiTariCoinPreview.feeValue))
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(vector, flags)
        parcel.writeParcelable(feeValue, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TariCoinPreview> {
        override fun createFromParcel(parcel: Parcel): TariCoinPreview {
            return TariCoinPreview(parcel)
        }

        override fun newArray(size: Int): Array<TariCoinPreview?> {
            return arrayOfNulls(size)
        }
    }
}