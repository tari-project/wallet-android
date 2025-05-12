package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class TariBaseNodeState(
    val heightOfLongestChain: BigInteger,
    val nodeId: String?,
) : Parcelable {

    constructor(ffiTariBaseNodeState: FFITariBaseNodeState) : this(
        heightOfLongestChain = ffiTariBaseNodeState.getHeightOfLongestChain(),
        nodeId = ffiTariBaseNodeState.getNodeId()?.hex(),
    )

    override fun toString() = "BaseNodeState(heightOfLongestChain=$heightOfLongestChain, nodeId=$nodeId)"
}