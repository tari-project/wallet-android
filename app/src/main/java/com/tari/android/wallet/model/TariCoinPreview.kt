package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariCoinPreview
import com.tari.android.wallet.ffi.FFITariVector
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class TariCoinPreview(
    val vector: TariVector,
    val feeValue: MicroTari,
) : Parcelable {
    constructor(ffiTariCoinPreview: FFITariCoinPreview) : this(
        vector = TariVector(FFITariVector(ffiTariCoinPreview.vectorPointer)),
        feeValue = MicroTari(BigInteger.valueOf(ffiTariCoinPreview.feeValue)),
    )
}