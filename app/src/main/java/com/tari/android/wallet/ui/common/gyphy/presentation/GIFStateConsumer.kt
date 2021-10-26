package com.tari.android.wallet.ui.common.gyphy.presentation

import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem

interface GIFStateConsumer {
    fun onLoadingState()
    fun onResourceReady()
    fun onErrorState()
    fun onSuccessState(gifItem: GIFItem)
    fun noGIFState()
}