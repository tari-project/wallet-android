package com.tari.android.wallet.data.sharedPrefs

import com.google.gson.Gson
import com.tari.android.wallet.data.sharedPrefs.network.NoSupportedNetworkException
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import com.tari.android.wallet.util.BroadcastEffectFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class CommonPrefRepository(
    private val applicationScope: CoroutineScope,
) : PrefUpdater {

    private val _effect = BroadcastEffectFlow<Effect>()

    override fun notifySettingsUpdated() {
        applicationScope.launch { _effect.send(Effect.SettingsUpdated) }
    }

    suspend fun doOnSettingsUpdated(action: () -> Unit) {
        _effect.flow.collect { action() }
    }

    fun TariNetwork.formatKey(key: String): String {
        val catching = runCatching { key + "_" + this.network.displayName }
        if (catching.isSuccess) {
            return catching.getOrNull().orEmpty()
        } else {
            try {
                val networkGson = Gson().toJson(this, TariNetwork::class.java)
                throw NoSupportedNetworkException(key + networkGson)
            } catch (e: Throwable) {
                throw NoSupportedNetworkException(key + e.message)
            }
        }
    }

    sealed class Effect() {
        data object SettingsUpdated : Effect()
    }
}

interface PrefUpdater {
    fun notifySettingsUpdated()
}