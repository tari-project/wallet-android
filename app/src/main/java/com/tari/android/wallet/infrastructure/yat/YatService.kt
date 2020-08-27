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

import com.tari.android.wallet.infrastructure.security.authentication.OAuthTokenPair
import com.tari.android.wallet.infrastructure.yat.authentication.AlternateIdAuthenticationRequestBody
import com.tari.android.wallet.infrastructure.yat.authentication.YatAuthenticationAPI
import com.tari.android.wallet.infrastructure.yat.cart.CartItemDTO
import com.tari.android.wallet.infrastructure.yat.cart.CartItemsDTO
import com.tari.android.wallet.infrastructure.yat.cart.CheckoutRequestBody
import com.tari.android.wallet.infrastructure.yat.cart.YatCartAPI
import com.tari.android.wallet.infrastructure.yat.emojiid.EmojiIdEditRequestBody
import com.tari.android.wallet.infrastructure.yat.emojiid.EmojiIdInsertDTO
import com.tari.android.wallet.infrastructure.yat.emojiid.TagTypeDTO
import com.tari.android.wallet.infrastructure.yat.emojiid.YatEmojiIdAPI
import com.tari.android.wallet.infrastructure.yat.user.UserRegistrationRequestBody
import com.tari.android.wallet.infrastructure.yat.user.YatUserAPI
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.User
import com.tari.android.wallet.model.UserNotFoundException
import com.tari.android.wallet.model.yat.ActualizingEmojiSet
import com.tari.android.wallet.model.yat.EmojiId
import com.tari.android.wallet.model.yat.EmojiSet
import com.tari.android.wallet.util.SharedPrefsWrapper
import retrofit2.Response

interface YatService {

    fun currentEmojiId(): EmojiId?

    fun doesUserExist(): Boolean

    fun isUserAuthenticated(): Boolean

    fun findAndReserveFreeYat(): EmojiId

    fun checkoutCart(pubkey: String)

    fun getUser(emojiId: EmojiId): User?

}

class RESTYatService(
    private val authenticationAPI: YatAuthenticationAPI,
    private val userAPI: YatUserAPI,
    private val emojiIdAPI: YatEmojiIdAPI,
    private val cartAPI: YatCartAPI,
    private val jwtStorage: YatJWTStorage,
    // TODO(nyarian): decorate or set tokens with interceptor
    private val userStorage: YatUserStorage,
    private val preferences: SharedPrefsWrapper,
    private val emojiSet: ActualizingEmojiSet,
) : YatService {

    override fun currentEmojiId(): EmojiId? = userStorage.get()?.emojiIds?.firstOrNull()

    override fun findAndReserveFreeYat(): EmojiId =
        makeSureThatUserIsAuthenticated()
            .also { emojiSet.actualize() }
            .run { findFreeYat(emojiSet) }
            .also(this::reserveYat)

    private fun makeSureThatUserIsAuthenticated() {
        val user = userStorage.get()
        val credentials = if (user != null) {
            YatCredentials(
                user.alternateId,
                user.password
            )
        } else {
            val credentials = registerUser()
            userStorage.put(YatUser(credentials.alternateId, credentials.password, emptySet()))
            credentials
        }
        if (!isUserAuthenticated()) {
            val tokens = authenticate(credentials.alternateId, credentials.password)
            jwtStorage.put(tokens)
        }
    }

    override fun doesUserExist() = userStorage.get() != null

    override fun isUserAuthenticated() = jwtStorage.accessToken() != null

    private fun registerUser(): YatCredentials =
        generateSequence {
            YatCredentials.withRandomPassword(preferences.publicKeyHexString!!)
                .run { Pair(this, UserRegistrationRequestBody(alternateId, password)) }
                .let { Pair(it.first, userAPI.register(it.second).execute()) }
        }
            .take(10)
            .first { it.second.isSuccessful }
            .first

    private fun findFreeYat(set: EmojiSet): EmojiId = generateSequence { YatAPI.randomEmojiId(set) }
        .map { Pair(it, emojiIdAPI.check(it.raw).execute()) }
        .take(20)
        .first { it.second.isSuccessful && it.second.body()!!.result.isAvailable }
        .first

    private fun reserveYat(emojiId: EmojiId) {
        cartAPI.clean().execute().checkIfSuccessful()
        cartAPI.add(CartItemsDTO(CartItemDTO(emojiId.raw))).execute().checkIfSuccessful()
    }

    override fun checkoutCart(pubkey: String) {
        val yat = cartAPI.get().execute().body()!!.orderItems.first { it.emojiId != null }.emojiId!!
        cartAPI.checkout(CheckoutRequestBody.free()).execute().checkIfSuccessful()
        val body = EmojiIdEditRequestBody(
            insert = listOf(EmojiIdInsertDTO(pubkey, TagTypeDTO.TARI_PUBKEY))
        )
        generateSequence { emojiIdAPI.edit(yat, body).execute() }
            .take(10)
            .first { it.isSuccessful }
        userStorage.addEmojiId(EmojiId.of(yat, emojiSet)!!)
    }

    private inline fun <reified T> Response<T>.checkIfSuccessful() =
        apply { check(isSuccessful) { errorBody()!!.string() } }

    private fun authenticate(alternateId: String, password: String): OAuthTokenPair {
        val request = AlternateIdAuthenticationRequestBody(alternateId, password)
        val response = authenticationAPI.authenticate(request).execute()
        if (!response.isSuccessful) throw RuntimeException(response.errorBody()!!.string())
        val result = response.body()!!
        return OAuthTokenPair(result.accessToken, result.refreshToken)
    }

    override fun getUser(emojiId: EmojiId): User? = emojiIdAPI.search(emojiId.raw).execute()
        .apply { if (!isSuccessful) throw UserNotFoundException(emojiId.raw) }
        .body()!!.result
        ?.firstOrNull { it.tag == TagTypeDTO.TARI_PUBKEY }
        ?.let { PublicKey(it.data, emojiId.raw) }
        ?.let(::User)
        ?.also { it.yat = emojiId.raw }

}
