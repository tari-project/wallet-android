package com.tari.android.wallet.ui.fragment.send.common

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User

data class TransactionData(val recipientUser: User?, val amount: MicroTari?, val note: String?, val feePerGram: MicroTari?, val isOneSidePayment: Boolean) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(User::class.java.classLoader),
        parcel.readParcelable(MicroTari::class.java.classLoader),
        parcel.readString(),
        parcel.readParcelable(MicroTari::class.java.classLoader) as? MicroTari,
        parcel.readInt() == 1
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(recipientUser, flags)
        parcel.writeParcelable(amount, flags)
        parcel.writeString(note)
        parcel.writeParcelable(feePerGram, flags)
        parcel.writeInt(if(isOneSidePayment) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TransactionData> {
        override fun createFromParcel(parcel: Parcel): TransactionData = TransactionData(parcel)

        override fun newArray(size: Int): Array<TransactionData?> = arrayOfNulls(size)
    }
}