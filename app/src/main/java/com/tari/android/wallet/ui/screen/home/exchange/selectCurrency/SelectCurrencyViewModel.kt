package com.tari.android.wallet.ui.screen.home.exchange.selectCurrency

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SelectCurrencyViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(selectedCurrency = savedState.getOrThrow<Exolix.Currency>(SelectCurrencyFragment.PRESELECTED_CURRENCY_KEY))
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<Effect>()
    val effect = _effect.flow

    private var searchJob: Job? = null

    init {
        loadMoreCurrencies()
    }

    fun onQueryChange(query: String) {
        if (query == uiState.value.searchQuery) return

        searchJob?.cancel()
        _uiState.update {
            it.hideLoading()
                .hideError()
                .copy(
                    searchQuery = query,
                    currentPage = 0,
                    currencies = emptyList(),
                    totalCount = null,
                )
        }
        loadMoreCurrencies()
    }

    fun onCurrencyItemClicked(currency: Exolix.Currency) {
        launchOnMain {
            _effect.send(Effect.SetSelectResult(currency))
            tariNavigator.navigateBack()
        }
    }

    fun onLoadMoreCurrencies() {
        loadMoreCurrencies()
    }

    private fun loadMoreCurrencies() {
        if (!uiState.value.hasMorePages || uiState.value.loading) return

        _uiState.update { it.showLoading() }
        searchJob = launchOnIo {
            val nextPage = uiState.value.currentPage + 1
            exolixRepository.getCurrencies(page = nextPage, size = 50, searchQuery = uiState.value.searchQuery)
                .onSuccess { response ->
                    _uiState.update {
                        it.hideLoading()
                            .hideError()
                            .copy(
                                currencies = it.currencies + response.data,
                                currentPage = nextPage,
                                totalCount = response.count,
                            )
                    }
                }.onFailure {
                    _uiState.update { it.hideLoading().showError(resourceManager.getString(R.string.exchange_load_more_currencies_error)) }
                }
        }
    }

    data class UiState(
        val currencies: List<Exolix.Currency> = emptyList(),
        val searchQuery: String = "",
        val errorMessage: String? = null,

        val currentPage: Int = 0,
        val loading: Boolean = false,

        val totalCount: Int? = null,
        val selectedCurrency: Exolix.Currency? = null,
    ) {
        val showEmptyState: Boolean
            get() = currencies.isEmpty() && !hasMorePages

        val hasMorePages: Boolean
            get() = totalCount == null || currencies.size < totalCount

        fun showLoading(): UiState = copy(loading = true)
        fun hideLoading(): UiState = copy(loading = false)
        fun showError(message: String): UiState = copy(errorMessage = message)
        fun hideError(): UiState = copy(errorMessage = null)
    }

    sealed class Effect {
        data class SetSelectResult(val selectedCurrency: Exolix.Currency) : Effect()
    }
}