package com.tari.android.wallet.ui.screen.home.exchange.selectNetwork

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SelectNetworkViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    init {
        component.inject(this)
    }

    private val networks = savedState.getOrThrow<ArrayList<Exolix.Network>>(SelectNetworkFragment.NETWORKS_KEY)
    private val preselectedNetwork = savedState.get<Exolix.Network>(SelectNetworkFragment.PRESELECTED_NETWORK_KEY)

    private val _uiState = MutableStateFlow(
        UiState(
            networks = networks,
            selectedNetwork = preselectedNetwork ?: networks.firstOrNull { it.isDefault } ?: networks.first(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<Effect>()
    val effect = _effect.flow

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onNetworkItemClicked(network: Exolix.Network) {
        launchOnMain {
            _effect.send(Effect.SetSelectResult(network))
            tariNavigator.navigateBack()
        }
    }

    data class UiState(
        val networks: List<Exolix.Network>,
        val selectedNetwork: Exolix.Network,
        val searchQuery: String = "",
    ) {
        val showEmptyState: Boolean
            get() = networks.isEmpty()

        val filteredNetworks: List<Exolix.Network>
            get() = networks.filter { network ->
                network.name.contains(searchQuery, ignoreCase = true) ||
                        network.network.contains(searchQuery, ignoreCase = true) ||
                        network.shortName?.contains(searchQuery, ignoreCase = true) == true
            }.sortedBy { it.name }
    }

    sealed class Effect {
        data class SetSelectResult(val selectedNetwork: Exolix.Network) : Effect()
    }
}