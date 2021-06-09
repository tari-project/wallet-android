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

import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.User
import com.tari.android.wallet.model.UserNotFoundException
import com.tari.android.wallet.model.yat.ActualizingEmojiSet
import com.tari.android.wallet.model.yat.EmojiId
import yat.android.data.YatRecordType

interface YatService {

    suspend fun saveYat(yatRaw: String)

    suspend fun lookupUser(emojiId: EmojiId): User?
}

class YatServiceImpl(
    private val yatAdapter: YatAdapter,
    private val userStorage: YatUserStorage,
    private val emojiSet: ActualizingEmojiSet,
) : YatService {

    override suspend fun saveYat(yatRaw: String) {
        emojiSet.actualize()

        val emojiSet = EmojiId.of(yatRaw, emojiSet)
        userStorage.addEmojiId(emojiSet!!)
    }

    override suspend fun lookupUser(emojiId: EmojiId): User? {
        val response = yatAdapter.lookupYatUser(emojiId.raw)

        if (response.error != null) {
            throw UserNotFoundException(emojiId.raw)
        }

        return response.response?.yatRecords?.firstOrNull { it.type == YatRecordType.TARI_PUBKEY }
            ?.let { PublicKey(it.data, emojiId.raw) }
            ?.let(::User)
            ?.also { u -> u.yat = emojiId.raw }
    }
}
