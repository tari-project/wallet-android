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
package com.tari.android.wallet.service.notification

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PushNotificationRESTService(
    private val gateway: PushNotificationRESTGateway,
    private val apiKey: String
) :
    NotificationService {
    override fun notifyRecipient(
        recipientPublicKeyHex: String,
        senderPublicKeyHex: String,
        signer: (String) -> String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val signing = signer(apiKey + senderPublicKeyHex + recipientPublicKeyHex)
        val signature = signing.split("|")[0]
        val nonce = signing.split("|")[1]
        val requestBody =
            PushNotificationRequestBody(
                senderPublicKeyHex,
                signature,
                nonce
            )
        val response = gateway.sendPushNotificationToRecipient(recipientPublicKeyHex, requestBody)
        response.enqueue(object : Callback<PushNotificationResponseBody> {
            override fun onFailure(call: Call<PushNotificationResponseBody>, t: Throwable) {
                onFailure(t)
            }

            override fun onResponse(
                call: Call<PushNotificationResponseBody>,
                response: Response<PushNotificationResponseBody>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    onSuccess()
                } else {
                    onFailure(NotificationException(response.errorBody()?.string()))
                }
            }
        })
    }

}
