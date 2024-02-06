package com.tari.android.wallet.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class EffectChannelFlow<Effect : Any> {
    private val channel: Channel<Effect> = Channel(Channel.CONFLATED)
    val flow: Flow<Effect> = channel.receiveAsFlow()

    suspend fun send(effect: Effect) {
        channel.send(effect)
    }
}