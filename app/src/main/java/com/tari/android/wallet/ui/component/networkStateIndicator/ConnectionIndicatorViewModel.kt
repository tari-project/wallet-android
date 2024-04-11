package com.tari.android.wallet.ui.component.networkStateIndicator

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModule
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule

class ConnectionIndicatorViewModel : CommonViewModel() {

    private val _networkState = MutableLiveData<NetworkConnectionState>()
    private val _torProxyState = MutableLiveData<TorProxyState>()
    private val _baseNodeState = MutableLiveData<BaseNodeState>()
    private val _syncState = MutableLiveData<BaseNodeSyncState>()

    private val _state = MediatorLiveData<ConnectionIndicatorState>()
    val state = _state.map { it }

    init {
        component.inject(this)

        _state.addSource(_torProxyState) { updateConnectionState() }
        _state.addSource(_networkState) { updateConnectionState() }
        _state.addSource(_baseNodeState) { updateConnectionState() }
        _state.addSource(_syncState) { updateConnectionState() }

        subscribeOnEventBus()
    }

    fun showStatesDialog(isRefreshing: Boolean = false) {
        _networkState.value ?: return
        _torProxyState.value ?: return
        _baseNodeState.value ?: return
        _syncState.value ?: return

        val args = ModularDialogArgs(
            DialogArgs(isRefreshing = isRefreshing), listOf(
                HeadModule(resourceManager.getString(R.string.connection_status_dialog_title)),
                ConnectionStatusesModule(_networkState.value!!, _torProxyState.value!!, _baseNodeState.value!!, _syncState.value!!),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun subscribeOnEventBus() {
        EventBus.torProxyState.subscribe(this) { _torProxyState.postValue(it) }
        EventBus.networkConnectionState.subscribe(this) { _networkState.postValue(it) }
        EventBus.baseNodeState.subscribe(this) { _baseNodeState.postValue(it) }
        EventBus.baseNodeSyncState.subscribe(this) { _syncState.postValue(it) }
    }

    private fun updateConnectionState() {
        _state.value = when (_networkState.value) {
            NetworkConnectionState.UNKNOWN,
            NetworkConnectionState.DISCONNECTED -> ConnectionIndicatorState.Disconnected

            NetworkConnectionState.CONNECTED -> {
                when (_torProxyState.value) {
                    is TorProxyState.Failed,
                    is TorProxyState.Initializing,
                    is TorProxyState.NotReady -> ConnectionIndicatorState.Disconnected

                    is TorProxyState.Running -> {
                        when (_baseNodeState.value) {
                            BaseNodeState.Online,
                            BaseNodeState.Syncing -> {
                                when (_syncState.value) {
                                    BaseNodeSyncState.Online,
                                    BaseNodeSyncState.Syncing -> ConnectionIndicatorState.Connected

                                    else -> ConnectionIndicatorState.ConnectedWithIssues
                                }
                            }

                            else -> ConnectionIndicatorState.Disconnected
                        }
                    }

                    else -> ConnectionIndicatorState.Disconnected
                }
            }

            else -> ConnectionIndicatorState.Disconnected
        }

        showStatesDialog(true)
    }
}