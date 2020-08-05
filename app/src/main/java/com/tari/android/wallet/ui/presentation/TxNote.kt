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
package com.tari.android.wallet.ui.presentation

internal class TxNote(val message: String?, val gifUrl: String?) {

    init {
        if (message == null && gifUrl == null) {
            throw IllegalStateException("Both message and gifUrl can't ge null at the same time")
        }
    }

    val gifId: String?
        get() = gifUrl?.split(Regex("/"))?.last()

    fun compose(): String = "${message ?: ""}${gifUrl?.run { " $this" } ?: ""}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TxNote
        if (message != other.message) return false
        if (gifUrl != other.gifUrl) return false
        return true
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (gifUrl?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "TransactionNote(message=$message, gifUrl=$gifUrl)"

    companion object {
        fun fromNote(
            note: String,
            assetsDomain: String = "giphy.com",
            protocol: String = "https://"
        ): TxNote {
            if (note.isEmpty()) throw IllegalStateException("Note can't be empty")
            val lines = note.split(Regex(" "))
            return if (Regex("$protocol$assetsDomain.*").matches(lines.last())) TxNote(
                message = lines.take(lines.size - 1).filter(String::isNotEmpty)
                    .joinToString(separator = " ")
                    .let { if (it.isEmpty()) null else it },
                gifUrl = lines.last()
            ) else TxNote(note, null)
        }
    }

}
