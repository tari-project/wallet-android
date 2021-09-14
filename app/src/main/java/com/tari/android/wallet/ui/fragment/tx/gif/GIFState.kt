package com.tari.android.wallet.ui.fragment.tx.gif

import com.tari.android.wallet.ui.presentation.gif.GIF

sealed class GIFState {
    open fun handle(consumer: GIFStateConsumer) = Unit

    object LoadingState : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.onLoadingState()
    }

    object ResourceReady : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.onResourceReady()
    }

    object ErrorState : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.onErrorState()
    }

    class SuccessState(private val gif: GIF) : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.onSuccessState(gif)
    }

    object NoGIFState : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.noGIFState()
    }
}