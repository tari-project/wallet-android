package com.tari.android.wallet.ui.screen.home.exchange

import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.ExchangeDirection
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.data.exolix.TARI_CURRENCY
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.filterNumbers
import com.tari.android.wallet.util.extension.filterSingleDot
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

private const val RATE_AUTO_REFRESH_INTERVAL_MS = 10_000L

class ExchangeViewModel : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var rateFetchJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        loadDefaultCurrency()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    fun onAmountChanged(amountValue: String) {
        val newAmount = amountValue.filterSingleDot().filterNumbers()
        if (newAmount == _uiState.value.amountValue) return

        _uiState.update { it.copy(amountValue = amountValue.filterSingleDot().filterNumbers()) }
        fetchRateIfValid()
    }

    fun onCurrencySelected(currency: CurrencyDto) {
        _uiState.update { it.copy(selectedCurrency = currency) }
        fetchRateIfValid()
    }

    fun onMinAmountClicked() {
        _uiState.value.rate?.minAmount?.let { onAmountChanged(it.toString()) }
    }

    fun onMaxAmountClicked() {
        _uiState.value.rate?.maxAmount?.let { onAmountChanged(it.toString()) }
    }

    fun onSelectCurrencyClicked() {
        tariNavigator.navigate(Navigation.Exchange.SelectCurrency(uiState.value.selectedCurrency))
    }

    fun onFixedRateToggled(fixedRate: Boolean) {
        _uiState.update { it.copy(fixedRate = fixedRate) }
        fetchRateIfValid()
    }

    fun onChangeDirectionClicked() {
        _uiState.update {
            it.copy(
                exchangeDirection = when (it.exchangeDirection) {
                    ExchangeDirection.BUY_TARI -> ExchangeDirection.SELL_TARI
                    ExchangeDirection.SELL_TARI -> ExchangeDirection.BUY_TARI
                }
            )
        }
        fetchRateIfValid()
    }

    fun onPullToRefresh() {
        fetchRateIfValid()
    }

    fun onRefreshClicked() {
        stopAutoRefresh()
        fetchRateIfValid()
    }

    fun onExchangeClicked() {
        stopAutoRefresh()

        val request = ExchangeRequestData(
            selectedCurrency = _uiState.value.selectedCurrency ?: error("selectedCurrency is null, but exchange button is not disabled"),
            selectedAddress = null, // TODO add address selection
            amount = _uiState.value.amount ?: error("amount is null, but exchange button is not disabled"),
            rate = _uiState.value.rate ?: error("rate is null, but exchange button is not disabled"),
            direction = _uiState.value.exchangeDirection,
        )

        tariNavigator.navigate(Navigation.Exchange.SendFunds(request))
    }

    fun loadDefaultCurrency() {
        _uiState.update { it.copy(loadingDefaultCurrency = true, loadingDefaultCurrencyError = false) }
        launchOnIo {
            exolixRepository.getCurrencies(page = 1, size = 1)
                .onSuccess { response ->
                    val defaultCurrency = response.currencies.firstOrNull()
                    if (defaultCurrency != null) {
                        onCurrencySelected(defaultCurrency)
                    }
                    _uiState.update { it.copy(loadingDefaultCurrency = false) }
                }.onFailure {
                    _uiState.update { state -> state.copy(loadingDefaultCurrency = false, loadingDefaultCurrencyError = true) }
                }
        }
    }

    private fun fetchRateIfValid() {
        rateFetchJob?.cancel()

        val amount = _uiState.value.amount
        val fromCurrency = _uiState.value.fromCurrency
        val toCurrency = _uiState.value.toCurrency

        if (_uiState.value.amount == null || fromCurrency == null || toCurrency == null) {
            _uiState.update { it.copy(rate = null, rateLoading = false) }
            stopAutoRefresh()
            return
        }

        _uiState.update { it.copy(rateLoading = true) }
        rateFetchJob = launchOnIo {
            exolixRepository.getRate(
                coinFrom = fromCurrency.coin,
                networkFrom = fromCurrency.networkName,
                coinTo = toCurrency.coin,
                networkTo = toCurrency.networkName,
                amount = amount.toString(),
                rateType = if (_uiState.value.fixedRate) Exolix.Rate.RateType.FIXED else Exolix.Rate.RateType.FLOATING,
            ).onSuccess { rate ->
                _uiState.update { it.copy(rate = rate, rateLoading = false) }
                startAutoRefresh()
            }.onFailure { e ->
                _uiState.update { it.copy(rateLoading = false) }

                showSimpleDialog(
                    title = resourceManager.getString(R.string.exchange_rate_error_title, WalletError(e).code),
                    description = resourceManager.getString(R.string.exchange_rate_error_message),
                )
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        _uiState.update { it.copy(autoRefreshActive = true) }
        autoRefreshJob = launchOnIo {
            delay(RATE_AUTO_REFRESH_INTERVAL_MS)
            fetchRateIfValid()
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        _uiState.update { it.copy(autoRefreshActive = false) }
    }

    data class UiState(
        val amountValue: String = "",

        val selectedCurrency: CurrencyDto? = null,

        val loadingDefaultCurrency: Boolean = false,
        val loadingDefaultCurrencyError: Boolean = false,
        val selectedToCurrency: Exolix.Currency? = null,
        val selectedToNetwork: Exolix.Network? = null,

        val fixedRate: Boolean = false,
        val rate: Exolix.Rate? = null,
        val rateLoading: Boolean = false,
        val autoRefreshActive: Boolean = false,

        val exchangeDirection: ExchangeDirection = ExchangeDirection.BUY_TARI,
    ) {
        val amount: BigDecimal?
            get() = runCatching { amountValue.toBigDecimal() }.getOrNull()

        val amountError: Boolean
            get() = rate != null && amount != null && (amount!! < rate.minAmount || amount!! > rate.maxAmount)

        val fromCurrency: CurrencyDto?
            get() = if (exchangeDirection == ExchangeDirection.BUY_TARI) selectedCurrency else TARI_CURRENCY

        val toCurrency: CurrencyDto?
            get() = if (exchangeDirection == ExchangeDirection.BUY_TARI) TARI_CURRENCY else selectedCurrency

        val exchangeButtonEnabled: Boolean
            get() = fromCurrency != null && toCurrency != null && amount != null && !amountError && !rateLoading
    }
}