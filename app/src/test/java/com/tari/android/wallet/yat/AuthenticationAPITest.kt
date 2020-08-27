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

import com.auth0.jwt.JWT
import com.tari.android.wallet.infrastructure.yat.authentication.AlternateIdAuthenticationRequestBody
import com.tari.android.wallet.infrastructure.yat.authentication.OAuthTokenPairResponseBody
import com.tari.android.wallet.infrastructure.yat.authentication.RefreshTokenRequestBody
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import retrofit2.Response

@Ignore("Should not be ran against a real API, so disabled by default")
class AuthenticationAPITest {

    private val gateway get() = YatAPITestResources.authenticationGateway

    @Test
    fun `alternate id authentication was successful with preexisting credentials`() {
        val response = authenticateWithCredentials()
        val body = response.body()!!
        assertTrue(response.isSuccessful)
        // assert that JWTDecodeException was not thrown, and, thus, JWTs are valid
        JWT.decode(body.accessToken)
        JWT.decode(body.refreshToken)
    }

    @Test
    fun `new token pair was returned if valid refresh token was provided`() {
        val refreshToken = authenticateWithCredentials().body()!!.refreshToken
        val response = gateway.refreshToken(RefreshTokenRequestBody(refreshToken)).execute()
        val body = response.body()!!
        assertTrue(response.isSuccessful)
        // assert that JWTDecodeException was not thrown, and, thus, JWTs are valid
        JWT.decode(body.accessToken)
        JWT.decode(body.refreshToken)
    }

    private fun authenticateWithCredentials(): Response<OAuthTokenPairResponseBody> {
        val body = AlternateIdAuthenticationRequestBody(
            PREEXISTING_ALTERNATE_ID,
            PREEXISTING_PASSWORD
        )
        return gateway.authenticate(body).execute()
    }
    private companion object {
        private const val PREEXISTING_ALTERNATE_ID = "4e06732134453e59a4254cfcaad582017eb1975a63e57bca2853c049653c0d07"
        private const val PREEXISTING_PASSWORD = "password"
    }

}
