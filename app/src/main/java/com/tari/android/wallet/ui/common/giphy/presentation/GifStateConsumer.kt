package com.tari.android.wallet.ui.common.giphy.presentation

import com.tari.android.wallet.ui.common.giphy.repository.GifItem

interface GifStateConsumer {
    fun onGifLoadingState()
    fun onGifResourceReady()
    fun onGifErrorState()
    fun onGifSuccessState(gifItem: GifItem)
    fun noGifState()
}