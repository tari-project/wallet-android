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
package com.tari.android.wallet.ui.presentation.gif

import android.net.Uri
import com.tari.android.wallet.ui.presentation.gif.dto.SearchGIFResponse
import com.tari.android.wallet.ui.presentation.gif.dto.SearchGIFsResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GIFRepository {
    fun getAll(query: String, limit: Int): List<GIF>

    fun getById(id: String): GIF
}

class GIFSearchException(s: String?) : IllegalStateException(s)

class GiphyRESTRetrofitRepository(private val gateway: GiphyRESTGateway) : GIFRepository {

    override fun getAll(query: String, limit: Int): List<GIF> {
        val response: Response<SearchGIFsResponse> = gateway.searchGIFs(query, limit).execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
            body.data.map {
                // TODO GIF OPTIMIZATION: change it.images.*variant* here to check other variants
                GIF(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.fixedWidth.url))
            }
        else {
            throw GIFSearchException(
                body?.meta?.message ?: response.message() ?: response.errorBody()?.string()
            )
        }
    }

    override fun getById(id: String): GIF {
        val response: Response<SearchGIFResponse> = gateway.getGIFByID(id).execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
        // TODO GIF OPTIMIZATION: change it.images.*variant* here to check other variants
            body.data.let {
                GIF(
                    it.id,
                    Uri.parse(it.embedUrl),
                    Uri.parse(it.images.fixedWidth.url)
                )
            }
        else {
            throw GIFSearchException(
                body?.meta?.message ?: response.message() ?: response.errorBody()?.string()
            )
        }
    }

}

interface GiphyRESTGateway {

    @GET("/v1/gifs/search")
    fun searchGIFs(@Query("q") query: String, @Query("limit") limit: Int): Call<SearchGIFsResponse>

    @GET("/v1/gifs/{gif_id}")
    fun getGIFByID(@Path("gif_id") id: String): Call<SearchGIFResponse>

}
