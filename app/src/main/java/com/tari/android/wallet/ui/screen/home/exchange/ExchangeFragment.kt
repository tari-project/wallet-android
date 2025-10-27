package com.tari.android.wallet.ui.screen.home.exchange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.home.exchange.selectCurrency.SelectCurrencyFragment
import com.tari.android.wallet.ui.screen.home.exchange.selectNetwork.SelectNetworkFragment
import com.tari.android.wallet.util.extension.composeContent
import com.tari.android.wallet.util.extension.parcelable

class ExchangeFragment : CommonFragment<ExchangeViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ExchangeScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onReloadDefCurrency = { viewModel.loadDefaultCurrency() },
                onAmountChanged = { viewModel.onAmountChanged(it) },
                onSelectCurrencyClicked = { viewModel.onSelectCurrencyClicked() },
                onSelectNetworkClicked = { viewModel.onSelectNetworkClicked() },
                onMinAmountClicked = { viewModel.onMinAmountClicked() },
                onMaxAmountClicked = { viewModel.onMaxAmountClicked() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ExchangeViewModel by viewModels()
        bindViewModel(viewModel)

        setFragmentResultListener(SelectCurrencyFragment.CURRENCY_REQUEST_KEY) { _, bundle ->
            val currency = bundle.parcelable<Exolix.Currency>(SelectCurrencyFragment.CURRENCY_RESULT_KEY)
            currency?.let { viewModel.onCurrencySelected(it) }
        }

        setFragmentResultListener(SelectNetworkFragment.NETWORK_REQUEST_KEY) { _, bundle ->
            val network = bundle.parcelable<Exolix.Network>(SelectNetworkFragment.NETWORK_RESULT_KEY)
            network?.let { viewModel.onNetworkSelected(it) }
        }
    }

    companion object {
        fun newInstance() = ExchangeFragment()
    }
}