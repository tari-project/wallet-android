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
import com.tari.android.wallet.infrastructure.yat.authentication.OAuthTokenPairResponseBody
import com.tari.android.wallet.infrastructure.yat.authentication.RefreshTokenRequestBody
import com.tari.android.wallet.infrastructure.yat.authentication.YatAuthenticationAPI
import okhttp3.Interceptor
import okhttp3.Response

// TODO(nyarian): Unit-test properly
class YatAuthenticationInterceptor(
    private val storage: YatJWTStorage,
    private val excludedEndpoints: List<String>,
    private val authenticationAPI: YatAuthenticationAPI,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        if (excludedEndpoints.any(chain.request().url.toString()::endsWith)) chain.proceed(chain.request())
        else storage.accessToken().let { token ->
            if (token == null) chain.proceed(chain.request()) // TODO(nyarian): refresh token
            else runOriginalRequest(chain, token)
        }

    private fun runOriginalRequest(chain: Interceptor.Chain, token: String): Response {
        val response = chain.proceed(
            chain.request().newBuilder()
                .header(HEADER_AUTHORIZATION, "${HEADER_PREFIX}$token")
                .build()
        )
        return if (response.code == CODE_UNAUTHORIZED)
            storage.refreshToken().let {
                if (it == null) response
                else refreshTokenAndRepeatRequest(it, chain, response)
            }
        else response
    }

    private fun refreshTokenAndRepeatRequest(
        refreshToken: String,
        chain: Interceptor.Chain,
        originalResponse: Response,
    ): Response {
        // TODO(nyarian): check if refresh token is expired. if yes, then clear the tokens pair
        // and return the failed response; otherwise update and do not clear the tokens on error
        val response = authenticationAPI.refreshToken(RefreshTokenRequestBody(refreshToken)).execute()
        return if (response.isSuccessful) {
            originalResponse.close()
            saveTokenAndRepeatRequest(response.body()!!, chain)
        } else originalResponse
    }

    private fun saveTokenAndRepeatRequest(
        body: OAuthTokenPairResponseBody,
        chain: Interceptor.Chain
    ): Response {
        storage.put(OAuthTokenPair(body.accessToken, body.refreshToken))
        return chain.proceed(
            chain.request().newBuilder()
                .header(HEADER_AUTHORIZATION, "${HEADER_PREFIX}${body.accessToken}")
                .build()
        )
    }

    private companion object {
        private const val CODE_UNAUTHORIZED = 401
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_PREFIX = "Bearer "
    }

}
