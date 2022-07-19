package com.tari.android.wallet.ui.component.networkStateIndicator

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.common.CommonViewModel

internal class ConnectionIndicatorViewModel : CommonViewModel() {

    private val _torProxyState = MutableLiveData<TorProxyState>()
    private val _networkState = MutableLiveData<NetworkConnectionState>()
    private val _baseNodeState = MutableLiveData<BaseNodeState>()

    private val _state = MediatorLiveData<ConnectionIndicatorState>()
    val state = Transformations.map(_state) { it }

    init {
        component.inject(this)

        _state.addSource(_torProxyState) { updateConnectionState() }
        _state.addSource(_networkState) { updateConnectionState() }
        _state.addSource(_baseNodeState) { updateConnectionState() }

        subscribeOnEventBus()
    }

    fun showStatesDialog() {
        //todo
    }

    private fun subscribeOnEventBus() {
        EventBus.torProxyState.subscribe(this) { _torProxyState.postValue(it) }
        EventBus.networkConnectionState.subscribe(this) { _networkState.postValue(it) }
        EventBus.baseNodeState.subscribe(this) { _baseNodeState.postValue(it) }
    }

    private fun updateConnectionState() {
        _state.value = when (_networkState.value) {
            NetworkConnectionState.UNKNOWN -> ConnectionIndicatorState.Disconnected
            NetworkConnectionState.DISCONNECTED -> ConnectionIndicatorState.Disconnected
            NetworkConnectionState.CONNECTED -> {
                when (_torProxyState.value) {
                    is TorProxyState.Failed -> ConnectionIndicatorState.Disconnected
                    TorProxyState.Initializing -> ConnectionIndicatorState.Disconnected
                    TorProxyState.NotReady -> ConnectionIndicatorState.Disconnected
                    is TorProxyState.Running -> {
                        when (_baseNodeState.value) {
                            is BaseNodeState.SyncStarted -> ConnectionIndicatorState.ConnectedWithIssues
                            is BaseNodeState.Online -> ConnectionIndicatorState.Connected
                            else -> ConnectionIndicatorState.Disconnected
                        }
                    }
                    else -> ConnectionIndicatorState.Disconnected
                }
            }
            else -> ConnectionIndicatorState.Disconnected
        }
    }
}