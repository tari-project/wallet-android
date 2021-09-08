package com.tari.android.wallet.ui.fragment.tx.gif

import com.tari.android.wallet.ui.presentation.gif.GIF

interface GIFStateConsumer {
    fun onLoadingState()
    fun onResourceReady()
    fun onErrorState()
    fun onSuccessState(gif: GIF)
    fun noGIFState()
}