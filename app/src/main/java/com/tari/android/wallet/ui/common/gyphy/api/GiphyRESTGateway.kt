package com.tari.android.wallet.ui.common.gyphy.api

import com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFResponse
import com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GiphyRESTGateway {

    @GET("/v1/gifs/search")
    fun searchGIFs(@Query("q") query: String, @Query("limit") limit: Int): Call<SearchGIFsResponse>

    @GET("/v1/gifs/{gif_id}")
    fun getGIFByID(@Path("gif_id") id: String): Call<SearchGIFResponse>

}