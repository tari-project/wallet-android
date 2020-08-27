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

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.yat.EmojiId

internal class TxMessagePayload private constructor(
    val message: String?,
    val gifUrl: String?,
    val sourceYat: EmojiId?,
    val destinationYat: EmojiId?,
    private val gson: Gson
) {

    val gifId: String?
        get() = gifUrl?.split(Regex("/"))?.last()

    fun otherPartyYat(direction: Tx.Direction): EmojiId? =
        if (direction == Tx.Direction.INBOUND) sourceYat else destinationYat

    fun compose(): String = gson.toJson(
        JSONNote(
            message,
            gifUrl,
            sourceYat?.raw,
            destinationYat?.raw
        )
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TxMessagePayload
        if (message != other.message) return false
        if (gifUrl != other.gifUrl) return false
        if (sourceYat != other.sourceYat) return false
        if (destinationYat != other.destinationYat) return false
        return true
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (gifUrl?.hashCode() ?: 0)
        result = 31 * result + (sourceYat?.hashCode() ?: 0)
        result = 31 * result + (destinationYat?.hashCode() ?: 0)
        return result
    }

    companion object {

        fun construct(
            message: String? = null,
            gifUrl: String? = null,
            source: EmojiId? = null,
            destination: EmojiId? = null,
            gson: Gson = Gson(),
        ): TxMessagePayload {
            check(message != null || gifUrl != null) { "Both message and gifUrl can't ge null at the same time" }
            return TxMessagePayload(
                message,
                gifUrl,
                source,
                destination,
                gson
            )
        }

        fun fromNote(note: String, gson: Gson = Gson()): TxMessagePayload =
            assumeJSONNoteVersion(note, gson) ?: assumeGiphyOnlyNoteVersion(note, gson)
            ?: TxMessagePayload(note, null, null, null, gson)

        private fun assumeGiphyOnlyNoteVersion(
            note: String,
            gson: Gson,
            assetsDomain: String = "giphy.com",
            protocol: String = "https://",
        ): TxMessagePayload? {
            val lines = note.split(Regex(" "))
            val matches = Regex("$protocol$assetsDomain.*").matches(lines.last())
            return if (!matches) null else
                TxMessagePayload(
                    message = lines.take(lines.size - 1).filter(String::isNotEmpty)
                        .joinToString(separator = " ")
                        .let { if (it.isEmpty()) null else it },
                    gifUrl = lines.last(),
                    sourceYat = null,
                    destinationYat = null,
                    gson = gson
                )
        }

        private fun assumeJSONNoteVersion(note: String, gson: Gson): TxMessagePayload? = try {
            gson.fromJson(note, JSONNote::class.java)
                ?.run {
                    TxMessagePayload(
                        text,
                        giphy,
                        from?.let(::EmojiId),
                        to?.let(::EmojiId),
                        gson
                    )
                }
        } catch (e: JsonSyntaxException) {
            null
        }

    }

    private data class JSONNote(
        @Expose @SerializedName("text") val text: String?,
        @Expose
        @SerializedName("giphy_url")
        val giphy: String?,
        @Expose
        @SerializedName("source_yat")
        val from: String?,
        @Expose
        @SerializedName("destination_yat")
        val to: String?,
    )

}
