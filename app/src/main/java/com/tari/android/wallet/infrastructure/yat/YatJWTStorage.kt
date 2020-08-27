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
package com.tari.android.wallet.infrastructure.yat

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.tari.android.wallet.infrastructure.security.authentication.OAuthTokenPair
import de.adorsys.android.securestoragelibrary.SecurePreferences

interface YatJWTStorage {

    fun accessToken(): String?

    fun refreshToken(): String?

    fun put(token: OAuthTokenPair)

    fun clear()

}

class PreferencesJWTStorage(private val context: Context, private val gson: Gson) : YatJWTStorage {
    override fun accessToken(): String? =
        getTokens(context)
            ?.let { gson.fromJson(it, SerializedTokens::class.java) }
            ?.accessToken

    override fun refreshToken(): String? =
        getTokens(context)
            ?.let { gson.fromJson(it, SerializedTokens::class.java) }
            ?.refreshToken

    override fun put(token: OAuthTokenPair) =
        setTokens(context, gson.toJson(SerializedTokens(token)))

    override fun clear() = removeTokens(context)

    // https://github.com/adorsys/secure-storage-android/issues/28#issuecomment-424394160
    private fun setTokens(context: Context, value: String) {
        value.chunked(SECURE_STORAGE_CHUNK_SIZE)
            .also { SecurePreferences.setValue(context, KEY_YAT_TOKEN_PAIR_CHUNKS, it.size) }
            .forEachIndexed { index, chunk ->
                SecurePreferences.setValue(context, "$KEY_YAT_TOKEN_PAIR$index", chunk)
            }
    }

    private fun getTokens(context: Context): String? {
        val numberOfChunks =
            SecurePreferences.getIntValue(context, KEY_YAT_TOKEN_PAIR_CHUNKS, 0)
        return if (numberOfChunks == 0) null
        else (0 until numberOfChunks).joinToString(separator = "") { index ->
            SecurePreferences.getStringValue(context, "$KEY_YAT_TOKEN_PAIR$index", null)!!
        }
    }

    private fun removeTokens(context: Context) {
        val numberOfChunks =
            SecurePreferences.getIntValue(context, KEY_YAT_TOKEN_PAIR_CHUNKS, 0)
        (0 until numberOfChunks).map {
            SecurePreferences.removeValue(context, "$KEY_YAT_TOKEN_PAIR$it")
        }
        SecurePreferences.removeValue(context, KEY_YAT_TOKEN_PAIR_CHUNKS)
    }

    data class SerializedTokens(
        @Expose @SerializedName("access_token") val accessToken: String,
        @Expose @SerializedName("refresh_token") val refreshToken: String,
    ) {
        constructor(tokens: OAuthTokenPair) : this(tokens.accessToken, tokens.refreshToken)
    }

    private companion object {
        private const val KEY_YAT_TOKEN_PAIR = "LLQHMXVGY761IRJP8OHMO95MHCXWY6MFLF7TTGIU"
        private const val KEY_YAT_TOKEN_PAIR_CHUNKS = "${KEY_YAT_TOKEN_PAIR}_O95MHCXWY6MFLF7TTGIU"
        private const val SECURE_STORAGE_CHUNK_SIZE = 240
    }

}
