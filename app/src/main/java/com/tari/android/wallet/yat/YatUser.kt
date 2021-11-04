package com.tari.android.wallet.yat

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.User

class YatUser() : User(), Parcelable {

    var yat: String = ""

    constructor(publicKey: PublicKey) : this() {
        this.publicKey = publicKey
    }

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        yat = parcel.readString().orEmpty()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeString(yat)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<YatUser> {
        override fun createFromParcel(parcel: Parcel): YatUser {
            return YatUser(parcel)
        }

        override fun newArray(size: Int): Array<YatUser?> {
            return arrayOfNulls(size)
        }
    }

    // endregion
}