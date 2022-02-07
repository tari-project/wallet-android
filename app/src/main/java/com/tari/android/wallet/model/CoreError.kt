package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

open class CoreError(var code: Int,var domain: String) : Serializable, Parcelable {
    val signature: String
        get() = "$domain-$code"

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString().orEmpty()
    )

    override fun equals(other: Any?): Boolean = (other as? CoreError)?.code == code

    override fun hashCode(): Int = code.hashCode()



    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(code)
        parcel.writeString(domain)
    }

    fun readFromParcel(inParcel: Parcel) {
        code = inParcel.readInt()
        domain = inParcel.readString().orEmpty()
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField val CREATOR = object : Parcelable.Creator<CoreError> {
            override fun createFromParcel(parcel: Parcel): CoreError = CoreError(parcel)

            override fun newArray(size: Int): Array<CoreError?> = arrayOfNulls(size)
        }
    }
}


