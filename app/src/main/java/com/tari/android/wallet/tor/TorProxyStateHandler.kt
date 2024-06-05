package com.tari.android.wallet.tor

import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.safeCastTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorProxyStateHandler @Inject constructor() {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val _torProxyState = MutableStateFlow<TorProxyState>(TorProxyState.NotReady)
    val torProxyState = _torProxyState.asStateFlow()

    fun updateState(state: TorProxyState) {
        _torProxyState.update { state }
    }

    suspend fun doOnTorBootstrapped(action: suspend (torState: TorProxyState.Running) -> Unit) = withContext(Dispatchers.IO) {
        torProxyState.firstOrNull { it is TorProxyState.Running && it.bootstrapStatus.progress == TorBootstrapStatus.MAX_PROGRESS }
            ?.safeCastTo<TorProxyState.Running>()
            ?.let {
                action(it)
            } ?: logger.i("Unable to reach Tor bootstrapped state")
    }

    suspend fun doOnTorRunning(action: suspend (torState: TorProxyState.Running) -> Unit) = withContext(Dispatchers.IO) {
        torProxyState.firstOrNull { it is TorProxyState.Running }
            ?.safeCastTo<TorProxyState.Running>()
            ?.let {
                action(it)
            } ?: logger.i("Unable to reach Tor running state")
    }

    suspend fun doOnTorFailed(action: suspend (torState: TorProxyState.Failed) -> Unit) = withContext(Dispatchers.IO) {
        torProxyState.firstOrNull { it is TorProxyState.Failed }
            ?.safeCastTo<TorProxyState.Failed>()
            ?.let {
                action(it)
            } ?: logger.i("Unable to reach Tor failed state")
    }
}