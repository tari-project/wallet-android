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
package com.tari.android.wallet.service.faucet

import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TestnetFaucetRESTService(
    private val gateway: TestnetFaucetRESTGateway,
    private val networkRepository: NetworkRepository
) : TestnetFaucetService {

    override fun requestMaxTestnetTari(
        publicKey: String,
        signature: String,
        publicNonce: String,
        onSuccess: (TestnetTariMaxAllocationResult) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val request = TestnetTariAllocateRequest(signature, publicNonce, networkRepository.currentNetwork!!.network.uriComponent)

        gateway.requestMaxTestnetTari(publicKey, request).enqueue(object : Callback<TestnetTariAllocateMaxResponse> {
            override fun onFailure(call: Call<TestnetTariAllocateMaxResponse>, t: Throwable) = onError(t)

            override fun onResponse(call: Call<TestnetTariAllocateMaxResponse>, response: Response<TestnetTariAllocateMaxResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    onSuccess(TestnetTariMaxAllocationResult(body.returnWalletId, body.keys))
                } else {
                    val message = "Status = ${response.code()}, ${response.errorBody()?.string()}"
                    onError(TestnetTariRequestException(message))
                }
            }
        })
    }
}
