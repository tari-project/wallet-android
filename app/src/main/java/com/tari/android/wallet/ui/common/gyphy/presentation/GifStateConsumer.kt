package com.tari.android.wallet.ui.common.gyphy.presentation

import com.tari.android.wallet.ui.common.gyphy.repository.GifItem

interface GifStateConsumer {
    fun onGifLoadingState()
    fun onGifResourceReady()
    fun onGifErrorState()
    fun onGifSuccessState(gifItem: GifItem)
    fun noGifState()
}