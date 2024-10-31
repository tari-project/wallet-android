package com.tari.android.wallet.ui.component.networkStateIndicator

import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.combineToPair
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.network.NetworkConnectionStateHandler
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ConnectionIndicatorViewModel : CommonViewModel() {

    @Inject
    lateinit var torProxyStateHandler: TorProxyStateHandler

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    @Inject
    lateinit var networkConnectionStateHandler: NetworkConnectionStateHandler

    @Inject
    lateinit var baseNodeStateHandler: BaseNodeStateHandler

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        component.inject(this)

        subscribeOnStates()
    }

    fun showStatesDialog(isRefreshing: Boolean = false) {
        if (!isRefreshing || dialogManager.isDialogShowing(ModularDialogArgs.DialogId.CONNECTION_STATUS)) {
            showModularDialog(
                ModularDialogArgs(
                    dialogId = ModularDialogArgs.DialogId.CONNECTION_STATUS,
                    modules = listOf(
                        HeadModule(resourceManager.getString(R.string.connection_status_dialog_title)),
                        ConnectionStatusesModule(
                            networkState = state.value.networkState,
                            torState = state.value.torProxyState,
                            baseNodeState = state.value.baseNodeState,
                            baseNodeSyncState = state.value.baseNodeSyncState,
                            walletScannedHeight = state.value.walletScannedHeight,
                            chainTip = state.value.chainTip,
                        ),
                        ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                    ),
                )
            )
        }
    }

    private fun subscribeOnStates() {
        collectFlow(networkConnectionStateHandler.networkConnectionState) { networkState ->
            _state.update { it.copy(networkState = networkState) }
            showStatesDialog(true)
        }
        collectFlow(baseNodeStateHandler.baseNodeState) { baseNodeState ->
            _state.update { it.copy(baseNodeState = baseNodeState) }
            showStatesDialog(true)
        }
        collectFlow(baseNodeStateHandler.baseNodeSyncState) { syncState ->
            _state.update { it.copy(baseNodeSyncState = syncState) }
            showStatesDialog(true)
        }
        collectFlow(torProxyStateHandler.torProxyState) { torProxyState ->
            _state.update { it.copy(torProxyState = torProxyState) }
            showStatesDialog(true)
        }

        collectFlow(baseNodesManager.walletScannedHeight.combineToPair(baseNodesManager.networkBlockHeight)) { (height, tip) ->
            _state.update {
                it.copy(
                    walletScannedHeight = height,
                    chainTip = tip.toInt(),
                )
            }
            showStatesDialog(true)
        }
    }

    data class UiState(
        val networkState: NetworkConnectionState = NetworkConnectionState.UNKNOWN,
        val torProxyState: TorProxyState = TorProxyState.NotReady,
        val baseNodeState: BaseNodeState = BaseNodeState.Offline,
        val baseNodeSyncState: BaseNodeSyncState = BaseNodeSyncState.NotStarted,
        val walletScannedHeight: Int = 0,
        val chainTip: Int = 0,
    ) {
        val indicatorState: ConnectionIndicatorState
            get() = when (networkState) {
                NetworkConnectionState.UNKNOWN,
                NetworkConnectionState.DISCONNECTED -> ConnectionIndicatorState.Disconnected

                NetworkConnectionState.CONNECTED -> {
                    when (torProxyState) {
                        is TorProxyState.Failed,
                        is TorProxyState.Initializing,
                        is TorProxyState.NotReady -> ConnectionIndicatorState.Disconnected

                        is TorProxyState.Running -> {
                            when (baseNodeState) {
                                BaseNodeState.Online,
                                BaseNodeState.Syncing -> {
                                    when (baseNodeSyncState) {
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
            }
    }
}