package com.tari.android.wallet.ui.common.gyphy.repository

import android.net.Uri
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.common.gyphy.api.GIFSearchException
import com.tari.android.wallet.ui.common.gyphy.api.GiphyRESTGateway
import com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFResponse
import com.tari.android.wallet.ui.common.gyphy.api.dto.SearchGIFsResponse
import retrofit2.Response

class GiphyRESTRetrofitRepository(private val gateway: GiphyRESTGateway) : GIFRepository {

    private val logger
        get() = Logger.t(GiphyRESTRetrofitRepository::class.simpleName)

    override fun getAll(query: String, limit: Int): List<GIFItem> {
        val response: Response<SearchGIFsResponse> = gateway.searchGIFs(query, limit).execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
            body.data.map {
                // TODO GIF OPTIMIZATION: change it.images.*variant* here to check other variants
                GIFItem(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.fixedWidth.url))
            }
        else {
            val exception = GIFSearchException(body?.meta?.message ?: response.message() ?: response.errorBody()?.string())
            logger.e(exception, "Get all was failed")
            throw exception
        }
    }

    override fun getById(id: String): GIFItem {
        val response: Response<SearchGIFResponse> = gateway.getGIFByID(id).execute()
        val body = response.body()
        return if (response.isSuccessful && body != null && body.meta.status in 200..299)
        // TODO GIF OPTIMIZATION: change it.images.*variant* here to check other variants
            body.data.let { GIFItem(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.fixedWidth.url)) }
        else {
            val exception = GIFSearchException(body?.meta?.message ?: response.message() ?: response.errorBody()?.string())
            logger.e(exception, "Get all was failed")
            throw exception
        }
    }
}