package com.tari.android.wallet.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A flow that can be used to send effects to the UI layer. Sends an effect to a single collector.
 */
class EffectFlow<Effect : Any> {
    private val channel: Channel<Effect> = Channel(Channel.CONFLATED)
    val flow: Flow<Effect> = channel.receiveAsFlow()

    suspend fun send(effect: Effect) {
        channel.send(effect)
    }
}

/**
 * A flow that can be used to send events across the app. Sends an event to multiple collectors.
 */
class BroadcastEffectFlow<Effect : Any> {
    private val _sharedFlow = MutableSharedFlow<Effect>(replay = 0)
    val flow: Flow<Effect> = _sharedFlow.asSharedFlow()

    suspend fun send(effect: Effect) {
        _sharedFlow.emit(effect)
    }
}