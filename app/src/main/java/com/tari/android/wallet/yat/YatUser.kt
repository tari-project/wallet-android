package com.tari.android.wallet.yat

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.User
import java.io.Serializable

class YatUser : User, Serializable {

    var yat: String = ""

    constructor(publicKey: PublicKey) : super() {
        this.publicKey = publicKey
    }

    // region Parcelable

    constructor(parcel: Parcel) : super() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<User> {

        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User> {
            return Array(size) { User() }
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(publicKey, flags)
    }

    private fun readFromParcel(inParcel: Parcel) {
        publicKey = inParcel.readParcelable(PublicKey::class.java.classLoader)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion
}