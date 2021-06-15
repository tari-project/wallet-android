package com.tari.android.wallet.infrastructure.yat.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import yat.android.YatLib
import yat.android.data.response.SupportedEmojiSetResponse
import yat.android.data.response.YatLookupResponse
import yat.android.data.storage.YatJWTStorage

interface YatAdapter {
    val state: LiveData<YatIntegrationStateDto>

    fun processDeeplink(context: Context, intent: Intent)

    fun start(intent: Activity)

    suspend fun lookupYatUser(yat: String) : YatResponse<YatLookupResponse>

    suspend fun getSupportedEmojiSet() : YatResponse<SupportedEmojiSetResponse>

    fun getJWTStorage() : YatJWTStorage

    enum class YatIntegrationState {
        None,
        Complete,
        Failed
    }

    data class YatIntegrationStateDto(
        val state: YatIntegrationState,
        val yat: String? = null,
        val failureType: YatLib.FailureType? = null
    )

    data class YatResponse<T>(
        val response: T?,
        val error: Throwable?
    )
}

