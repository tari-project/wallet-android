package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable

open class WalletError : CoreError {

    constructor() : super(NoError.code, "FFI")

    constructor(code: Int) : super(code, "FFI")

    constructor(parcel: Parcel) : super(parcel)

    class CommonError(code: Int): WalletError(code)

    object DatabaseDataError : WalletError(114)
    object TransactionNotFoundError : WalletError(204)
    object ContactNotFoundError : WalletError(401)
    object InvalidPassphraseEncryptionCypherError : WalletError(420)
    object InvalidPassphraseError : WalletError(428)
    object SeedWordsInvalidDataError : WalletError(429)
    object SeedWordsVersionMismatchError : WalletError(430)
    object UnknownError : WalletError(-1)

    object NoError : WalletError(0)



    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(code)
        parcel.writeString(domain)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField val CREATOR = object : Parcelable.Creator<CoreError> {
            override fun createFromParcel(parcel: Parcel): CoreError = CoreError(parcel)

            override fun newArray(size: Int): Array<CoreError?> = arrayOfNulls(size)
        }
    }
}

internal fun throwIf(error: CoreError?) {
    if (error != null && error.code != 0) {
        throw WalletException(error)
    }
}

class WalletException(val walletError: CoreError) : RuntimeException(walletError.signature) {
    override fun toString(): String = "Unhandled wallet error: ${walletError.code}"
}