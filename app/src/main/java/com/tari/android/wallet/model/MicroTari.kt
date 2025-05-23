/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Parcelize
data class MicroTari(val value: BigInteger) : Parcelable, Comparable<MicroTari>, Serializable {

    val tariValue: BigDecimal
        // Note: BigDecimal keeps track of both precision and scale, 1e6 != 1_000_000 in this case (scale 6, scale 0)
        get() = value.toBigDecimal().divide(precisionValue, 6, RoundingMode.HALF_UP)

    val formattedTariValue: String
        get() = getFormattedValue(tariValue.toString())

    val formattedValue: String
        get() = getFormattedValue(value.toBigDecimal().setScale(6).toString())

    private fun getFormattedValue(value: String): String = value.trimEnd { it == '0' }.trimEnd { it == '.' }.trimEnd { it == ',' }

    operator fun plus(increment: MicroTari): MicroTari = MicroTari(this.value + increment.value)

    operator fun plus(increment: Int): MicroTari = this + increment.toMicroTari()

    operator fun plus(increment: Long): MicroTari = this + increment.toMicroTari()

    operator fun minus(decrement: MicroTari): MicroTari = MicroTari(this.value - decrement.value)

    operator fun minus(decrement: Int): MicroTari = this - decrement.toMicroTari()

    operator fun minus(decrement: Long): MicroTari = this - decrement.toMicroTari()

    override fun compareTo(other: MicroTari): Int = this.value.compareTo(other.value)

    companion object {
        val precisionValue: BigDecimal = BigDecimal.valueOf(1_000_000)
    }
}