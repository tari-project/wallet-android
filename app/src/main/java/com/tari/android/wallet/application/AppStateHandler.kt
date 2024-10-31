package com.tari.android.wallet.application

import com.tari.android.wallet.event.EffectChannelFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateHandler @Inject constructor() {

    private val _appEvent = EffectChannelFlow<AppEvent>()
    val appEvent: Flow<AppEvent> = _appEvent.flow

    suspend fun sendAppBackgrounded() = withContext(Dispatchers.Main) {
        _appEvent.send(AppEvent.AppBackgrounded)
    }

    suspend fun sendAppForegrounded() = withContext(Dispatchers.Main) {
        _appEvent.send(AppEvent.AppForegrounded)
    }

    sealed class AppEvent {
        data object AppBackgrounded : AppEvent()
        data object AppForegrounded : AppEvent()
    }
}