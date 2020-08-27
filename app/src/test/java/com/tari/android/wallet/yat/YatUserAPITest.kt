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
import com.tari.android.wallet.infrastructure.yat.YatAPIErrorDTO
import com.tari.android.wallet.infrastructure.yat.YatCredentials
import com.tari.android.wallet.infrastructure.yat.user.UserRegistrationRequestBody
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

@Ignore("Should not be ran against a real API, so disabled by default")
class YatUserAPITest {

    private val gateway get() = YatAPITestResources.userGateway

    @Test
    fun `registration was successful for a unique alternate id`() {
        val (email, response) = generateSequence { YatCredentials.random() }
            .map { UserRegistrationRequestBody(it.alternateId, it.password) }
            .map { it.alternate_id to gateway.register(it).execute() }
            .onEach { (_, response) ->
                if (!response.isSuccessful) println(response.errorBody()!!.string())
            }
            .take(10)
            .first { it.second.isSuccessful }
        assertEquals(email, response.body()!!.user.email)
    }

    @Test
    fun `duplication error was returned if already user email address was used`() {
        val body = UserRegistrationRequestBody(USED_EMAIL_ADDRESS, "password")
        val response = gateway.register(body).execute()
            .let { if (it.isSuccessful) gateway.register(body).execute() else it }
        val errorBody = gson.fromJson(
            response.errorBody()!!.string(),
            YatAPIErrorDTO::class.java
        )
        assertEquals(ERROR_CODE_UNIQUENESS, errorBody.field("email").single()["code"])
    }

    private companion object {
        private const val ERROR_CODE_UNIQUENESS = "uniqueness"
        private const val USED_EMAIL_ADDRESS = "email@email.email"
        private val gson = Gson()
    }

}
