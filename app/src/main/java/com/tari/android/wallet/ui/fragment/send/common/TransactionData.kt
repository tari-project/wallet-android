package com.tari.android.wallet.ui.fragment.send.common

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User

data class TransactionData(val recipientUser: User?, val amount: MicroTari?, val note: String?) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(User::class.java.classLoader),
        parcel.readParcelable(MicroTari::class.java.classLoader),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(recipientUser, flags)
        parcel.writeParcelable(amount, flags)
        parcel.writeString(note)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionData> {
        override fun createFromParcel(parcel: Parcel): TransactionData {
            return TransactionData(parcel)
        }

        override fun newArray(size: Int): Array<TransactionData?> {
            return arrayOfNulls(size)
        }
    }
}