package com.tari.android.wallet.ui.screen.home.exchange.selectCurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.composeContent

class SelectCurrencyFragment : CommonFragment<SelectCurrencyViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            SelectCurrencyScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onSearchQueryChange = { viewModel.onQueryChange(it) },
                onCurrencyItemClick = { viewModel.onCurrencyItemClicked(it) },
                onLoadMoreCurrencies = { viewModel.onLoadMoreCurrencies() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: SelectCurrencyViewModel by viewModels()
        bindViewModel(viewModel)

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is SelectCurrencyViewModel.Effect.SetSelectResult -> {
                    setFragmentResult(CURRENCY_REQUEST_KEY, Bundle().apply { putParcelable(CURRENCY_RESULT_KEY, effect.selectedCurrency) })
                }
            }
        }
    }

    companion object {
        const val CURRENCY_REQUEST_KEY = "CURRENCY_REQUEST_KEY"
        const val CURRENCY_RESULT_KEY = "CURRENCY_RESULT_KEY"

        const val PRESELECTED_CURRENCY_KEY = "PRESELECTED_CURRENCY_KEY"

        fun newInstance(preselectedCurrency: Exolix.Currency? = null) = SelectCurrencyFragment().apply {
            arguments = Bundle().apply {
                putParcelable(PRESELECTED_CURRENCY_KEY, preselectedCurrency)
            }
        }
    }
}