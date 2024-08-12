package com.tari.android.wallet.ui.common.gyphy.presentation

import com.tari.android.wallet.ui.common.gyphy.repository.GifItem

sealed class GifState {
    open fun handle(consumer: GifStateConsumer) = Unit

    data object LoadingState : GifState() {
        override fun handle(consumer: GifStateConsumer) = consumer.onGifLoadingState()
    }

    data object ResourceReady : GifState() {
        override fun handle(consumer: GifStateConsumer) = consumer.onGifResourceReady()
    }

    data object ErrorState : GifState() {
        override fun handle(consumer: GifStateConsumer) = consumer.onGifErrorState()
    }

    data class SuccessState(private val gifItem: GifItem) : GifState() {
        override fun handle(consumer: GifStateConsumer) = consumer.onGifSuccessState(gifItem)
    }

    data object NoGIFState : GifState() {
        override fun handle(consumer: GifStateConsumer) = consumer.noGifState()
    }
}