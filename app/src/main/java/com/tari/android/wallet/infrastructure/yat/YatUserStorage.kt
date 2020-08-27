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
import com.tari.android.wallet.model.yat.EmojiId
import de.adorsys.android.securestoragelibrary.SecurePreferences

interface YatUserStorage {

    fun get(): YatUser?

    fun addEmojiId(emojiId: EmojiId)

    fun put(user: YatUser)

    fun clear()

}

class PreferencesGSONUserStorage(private val context: Context, private val gson: Gson) :
    YatUserStorage {

    override fun get(): YatUser? =
        SecurePreferences.getStringValue(context, KEY_USER, null)
            ?.let { gson.fromJson(it, JsonUser::class.java) }
            ?.run { YatUser(alternateId, password, emojiIds.map(::EmojiId).toSet()) }

    override fun addEmojiId(emojiId: EmojiId) =
        get()!!.run { put(copy(emojiIds = emojiIds + emojiId)) }

    override fun put(user: YatUser) {
        gson.toJson(user.run { JsonUser(alternateId, password, emojiIds.map(EmojiId::raw)) })
            .let { SecurePreferences.setValue(context, KEY_USER, it) }
    }

    override fun clear() {
        SecurePreferences.removeValue(context, KEY_USER)
    }

    companion object {
        private const val KEY_USER = "GBFIJENZG8TWM39MC4IZK93ZY977421ARF3NPZVR"
    }

    private data class JsonUser(
        @Expose @SerializedName("alternate_id") val alternateId: String,
        @Expose @SerializedName("password") val password: String,
        @Expose @SerializedName("emoji_ids") val emojiIds: List<String>,
    )

}
