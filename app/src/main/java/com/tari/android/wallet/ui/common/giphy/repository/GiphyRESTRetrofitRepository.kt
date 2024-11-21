package com.tari.android.wallet.ui.common.giphy.repository

import android.net.Uri
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.common.giphy.api.GIFSearchException
import com.tari.android.wallet.ui.common.giphy.api.GiphyRESTGateway
import com.tari.android.wallet.ui.common.giphy.api.dto.SearchGIFResponse
import com.tari.android.wallet.ui.common.giphy.api.dto.SearchGIFsResponse
import retrofit2.Response

class GiphyRESTRetrofitRepository(private val gateway: GiphyRESTGateway) : GifRepository {

    private val logger
        get() = Logger.t(GiphyRESTRetrofitRepository::class.simpleName)

    override fun getAll(query: String, limit: Int): List<GifItem> {
        val response: Response<SearchGIFsResponse> = gateway.searchGIFs(query, limit).execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
            body.data.map { GifItem(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.fixedWidth.url)) }
        else {
            val exception = GIFSearchException(body?.meta?.message ?: response.message() ?: response.errorBody()?.string())
            logger.i(exception.toString() + "Get all was failed")
            throw exception
        }
    }

    override fun getById(id: String): GifItem {
        val request = gateway.getGIFByID(id)
        val response: Response<SearchGIFResponse> = request.execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
            body.data.let { GifItem(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.fixedWidth.url)) }
        else {
            val exception = GIFSearchException(body?.meta?.message ?: response.message() ?: response.errorBody()?.string())
            logger.i(exception.message.orEmpty())
            logger.i(exception.stackTraceToString())
            logger.i(exception. toString() + "Get by id was failed")
            throw exception
        }
    }
}