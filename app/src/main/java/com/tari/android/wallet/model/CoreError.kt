package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
open class CoreError(
    open var code: Int,
    open var domain: String,
) : Serializable, Parcelable {
    val signature: String
        get() = "$domain-$code"

    override fun equals(other: Any?): Boolean = (other as? CoreError)?.code == code

    override fun hashCode(): Int = code.hashCode()

    fun readFromParcel(inParcel: Parcel) {
        code = inParcel.readInt()
        domain = inParcel.readString().orEmpty()
    }
}


