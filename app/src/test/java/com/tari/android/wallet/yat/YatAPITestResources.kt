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
package com.tari.android.wallet.yat

import com.google.gson.Gson
import com.tari.android.wallet.infrastructure.security.authentication.OAuthTokenPair
import com.tari.android.wallet.infrastructure.yat.YatAuthenticationInterceptor
import com.tari.android.wallet.infrastructure.yat.YatCredentials
import com.tari.android.wallet.infrastructure.yat.YatJWTStorage
import com.tari.android.wallet.infrastructure.yat.authentication.AlternateIdAuthenticationRequestBody
import com.tari.android.wallet.infrastructure.yat.authentication.OAuthTokenPairResponseBody
import com.tari.android.wallet.infrastructure.yat.authentication.YatAuthenticationAPI
import com.tari.android.wallet.infrastructure.yat.cart.YatCartAPI
import com.tari.android.wallet.infrastructure.yat.key.YatPubkeyAPI
import com.tari.android.wallet.infrastructure.yat.user.UserRegistrationRequestBody
import com.tari.android.wallet.infrastructure.yat.user.UserRegistrationResponseBody
import com.tari.android.wallet.infrastructure.yat.user.YatUserAPI
import com.tari.android.wallet.model.yat.EmojiSet
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicReference

object YatAPITestResources {

    private const val BASE_URL = "https://activated.scratch.emojid.me/api/"
    val gson = Gson()
    private val nakedRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    val authenticationGateway: YatAuthenticationAPI =
        nakedRetrofit.create(YatAuthenticationAPI::class.java)
    val userGateway: YatUserAPI = nakedRetrofit.create(YatUserAPI::class.java)
    val cartGateway: YatCartAPI
    val pubkeyGateway: YatPubkeyAPI

    val yatEmojiSet: EmojiSet = InMemoryEmojiSet(
        arrayOf(
            "🀄", "🃏", "🆒", "🆓", "🆔", "🆘", "🆙", "🆚", "🌈", "🌊", "🌋", "🌕", "🌙", "🌪", "🌬",
            "🌭", "🌮", "🌯", "🌰", "🌲", "🌴", "🌵", "🌶", "🌸", "🌹", "🌽", "🍀", "🍁", "🍄", "🍆", "🍇",
            "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🍎", "🍐", "🍑", "🍒", "🍓", "🍔", "🍕", "🍗", "🍘", "🍚",
            "🍜", "🍝", "🍞", "🍟", "🍡", "🍣", "🍥", "🍦", "🍩", "🍪", "🍫", "🍬", "🍭", "🍯", "🍱", "🍳",
            "🍴", "🍵", "🍶", "🍷", "🍸", "🍹", "🍺", "🍼", "🍾", "🍿", "🎀", "🎁", "🎂", "🎃", "🎄", "🎈",
            "🎉", "🎋", "🎎", "🎏", "🎐", "🎒", "🎓", "🎟", "🎠", "🎡", "🎢", "🎣", "🎤", "🎥", "🎧", "🎨",
            "🎩", "🎪", "🎬", "🎭", "🎮", "🎰", "🎱", "🎲", "🎳", "🎵", "🎷", "🎸", "🎹", "🎺", "🎻",
            "🎼", "🎽", "🎾", "🎿", "🏀", "🏂", "🏆", "🏈", "🏉", "🏍", "🏎", "🏏", "🏐", "🏒", "🏓", "🏛",
            "🏟", "🏠", "🏥", "🏦", "🏧", "🏭", "🏮", "🏯", "🏰", "🏸", "🏹", "🏺", "🐀", "🐃", "🐊", "🐌",
            "🐍", "🐐", "🐑", "🐔", "🐗", "🐘", "🐙", "🐚", "🐛", "🐜", "🐝", "🐞", "🐢", "🐣", "🐦", "🐨",
            "🐪", "🐬", "🐭", "🐮", "🐯", "🐰", "🐱", "🐲", "🐳", "🐴", "🐵", "🐶", "🐷", "🐸", "🐺",
            "🐻", "🐼", "🐽", "🐾", "👀", "👁", "👂", "👃", "👅", "👋", "👌", "👍", "👎", "👏", "👑", "👒",
            "👔", "👖", "👗", "👘", "👙", "👛", "👞", "👟", "👠", "👢", "👣", "👻", "👽", "👾", "💃", "💄",
            "💅", "💈", "💉", "💊", "💍", "💎", "💐", "💔", "💡", "💣", "💦", "💩", "💪", "💯", "💰", "💱",
            "💳", "💸", "💺", "💻", "💼", "💾", "📈", "📌", "📎", "📏", "📐", "📓", "📚", "📜", "📟", "📡",
            "📦", "📱", "📷", "📺", "📻", "📿", "🔋", "🔌", "🔑", "🔒", "🔔", "🔥", "🔦", "🔩", "🔪", "🔫",
            "🔬", "🔭", "🔮", "🔱", "🕊", "🕌", "🕍", "🕯️", "🕳", "🕷", "🕸", "🖍", "🖖", "🖨", "🖼", "🗄",
            "🗑", "🗞", "🗡", "🗺", "🗼", "🗽", "🗾", "🗿", "😂", "😇", "😈", "😍", "😎", "😶", "😷",
            "😻", "😾", "😿", "🙃", "🙈", "🙌", "🙏", "🚀", "🚁", "🚑", "🚒", "🚓", "🚕", "🚗", "🚚",
            "🚜", "🚠", "🚢", "🚦", "🚧", "🚨", "🚩", "🚪", "🚬", "🚰", "🚲", "🚽", "🚿", "🛋", "🛍", "🛡",
            "🛢", "🤐", "🤑", "🤒", "🤓", "🤔", "🤕", "🤖", "🤘", "🦀", "🦁", "🦂", "🦃", "🦄", "🧀", "⌚",
            "⌛", "☀️", "☁️", "☂️", "☄️", "☕", "☠️", "☢️", "☦️", "☪️", "☯️", "☸️", "♟️", "♠️", "♣️", "♻️",
            "⚓", "⚖️", "⚙️", "⚛️", "⚜️", "⚡", "⚰️", "⚽", "⚾", "⛓️", "⛪", "⛳", "⛵", "⛷", "⛸", "✂️",
            "✈️", "✉️", "✌️", "✍️", "✏️", "✝️", "✡️", "❄️", "❓", "❗", "❤️", "⭐"
        ).toSet())

    init {
        val user = createAccount()
        val response = authenticationGateway
            .authenticate(
                AlternateIdAuthenticationRequestBody(user.user.alternateId, "password")
            ).execute()
        check(response.isSuccessful) { response.errorBody()!!.string() }
        val tokens = response.body()!!
        cartGateway = createCartGateway(tokens)
        pubkeyGateway = createPubkeyGateway(tokens)
    }

    private fun createAccount(): UserRegistrationResponseBody =
        generateSequence { YatCredentials.random() }
            .map { UserRegistrationRequestBody(it.alternateId, it.password) }
            .map { userGateway.register(it).execute() }
            .onEach { if (!it.isSuccessful) println(it.errorBody()!!.string()) }
            .take(10)
            .first { it.isSuccessful }
            .body()!!

    private fun createCartGateway(tokens: OAuthTokenPairResponseBody) =
        createAuthenticatingRetrofit(tokens).create(YatCartAPI::class.java)

    private fun createPubkeyGateway(tokens: OAuthTokenPairResponseBody) =
        createAuthenticatingRetrofit(tokens).create(YatPubkeyAPI::class.java)

    private fun createAuthenticatingRetrofit(tokens: OAuthTokenPairResponseBody): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createCartHttpClient(tokens))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createCartHttpClient(tokens: OAuthTokenPairResponseBody): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                YatAuthenticationInterceptor(
                    InMemoryStorage(tokens.accessToken, tokens.refreshToken),
                    emptyList(),
                    authenticationGateway
                )
            )
            .build()

    private class InMemoryStorage(accessToken: String?, refreshToken: String?) : YatJWTStorage {

        private val _accessToken = AtomicReference<String>(accessToken)
        private val _refreshToken = AtomicReference<String>(refreshToken)

        override fun accessToken(): String? = _accessToken.get()

        override fun refreshToken(): String? = _refreshToken.get()

        override fun put(token: OAuthTokenPair) {
            _accessToken.set(token.accessToken)
            _refreshToken.set(token.refreshToken)
        }

        override fun clear() {
            _accessToken.set(null)
            _refreshToken.set(null)
        }

    }

    private class InMemoryEmojiSet(override val set: Set<String>): EmojiSet

}
