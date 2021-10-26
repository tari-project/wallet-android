package com.tari.android.wallet.ui.common.gyphy.presentation

import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem

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

    class SuccessState(private val gifItem: GIFItem) : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.onSuccessState(gifItem)
    }

    object NoGIFState : GIFState() {
        override fun handle(consumer: GIFStateConsumer) = consumer.noGIFState()
    }
}