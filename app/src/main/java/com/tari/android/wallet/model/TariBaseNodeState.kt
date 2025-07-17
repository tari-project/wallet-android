package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class TariBaseNodeState(
    val heightOfLongestChain: BigInteger,
) : Parcelable {

    val isSynced: Boolean
        get() = heightOfLongestChain > BigInteger.ZERO

    constructor(ffiTariBaseNodeState: FFITariBaseNodeState) : this(
        heightOfLongestChain = ffiTariBaseNodeState.getHeightOfLongestChain(),
    )

    override fun toString() = "BaseNodeState(heightOfLongestChain=$heightOfLongestChain)"
}