package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariUtxo
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
data class TariUtxo(
    val commitment: String = "",
    val value: MicroTari = MicroTari(BigInteger.ZERO),
    val minedHeight: Long = -1,
    val timestamp: Long = -1,
    val status: UtxoStatus = UtxoStatus.Spent,
) : Parcelable {

    constructor(ffiUtxo: FFITariUtxo) : this(
        commitment = ffiUtxo.commitment,
        value = MicroTari(BigInteger.valueOf(ffiUtxo.value)),
        minedHeight = ffiUtxo.minedHeight,
        timestamp = ffiUtxo.minedTimestamp,
        status = UtxoStatus.fromValue(ffiUtxo.status.toInt()),
    )

    enum class UtxoStatus(val value: Int) {
        Unspent(0),
        Spent(1),
        EncumberedToBeReceived(2),
        EncumberedToBeSpent(3),
        Invalid(4),
        CancelledInbound(5),
        UnspentMinedUnconfirmed(6),
        ShortTermEncumberedToBeReceived(7),
        ShortTermEncumberedToBeSpent(8),
        SpentMinedUnconfirmed(9),
        AbandonedCoinbase(10),
        NotStored(11);

        companion object {
            fun fromValue(value: Int): UtxoStatus = entries.first { it.value == value }
        }
    }
}