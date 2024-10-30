package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.ffi.FFIFeePerGramStats
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.NetworkSpeed
import kotlin.math.min

data class FeePerGramOptions(val networkSpeed: NetworkSpeed, val slow: MicroTari, val medium: MicroTari, val fast: MicroTari) {

    constructor(ffiFeePerGramStats: FFIFeePerGramStats) : this(
        networkSpeed = when (min(ffiFeePerGramStats.getLength(), 3)) {
            1 -> NetworkSpeed.Slow
            2 -> NetworkSpeed.Medium
            3 -> NetworkSpeed.Fast
            else -> throw Exception("Unexpected block count")
        },
        slow = when (min(ffiFeePerGramStats.getLength(), 3)) {
            1 -> ffiFeePerGramStats.getAt(0).getMin()
            2 -> ffiFeePerGramStats.getAt(1).getAverage()
            3 -> ffiFeePerGramStats.getAt(2).getAverage()
            else -> throw Exception("Unexpected block count")
        }.let { MicroTari(it) },
        medium = when (min(ffiFeePerGramStats.getLength(), 3)) {
            1 -> ffiFeePerGramStats.getAt(0).getAverage()
            2 -> ffiFeePerGramStats.getAt(0).getMin()
            3 -> ffiFeePerGramStats.getAt(1).getAverage()
            else -> throw Exception("Unexpected block count")
        }.let { MicroTari(it) },
        fast = when (min(ffiFeePerGramStats.getLength(), 3)) {
            1 -> ffiFeePerGramStats.getAt(0).getMax()
            2 -> ffiFeePerGramStats.getAt(0).getMax()
            3 -> ffiFeePerGramStats.getAt(0).getMax()
            else -> throw Exception("Unexpected block count")
        }.let { MicroTari(it) },
    )
}
