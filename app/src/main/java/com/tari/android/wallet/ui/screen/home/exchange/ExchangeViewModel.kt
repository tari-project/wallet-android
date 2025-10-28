package com.tari.android.wallet.ui.screen.home.exchange

import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.filterNumbers
import com.tari.android.wallet.util.extension.filterSingleDot
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

class ExchangeViewModel : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var rateFetchJob: Job? = null

    init {
        loadDefaultCurrency()
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

    fun onExchangeClicked() {
        showNotReadyYetDialog()
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
        val amount = _uiState.value.amount
        val toCurrency = _uiState.value.toCurrency
        val fromCurrency = _uiState.value.fromCurrency

        if (!_uiState.value.isAmountValid || toCurrency == null || fromCurrency == null) {
            // Clear rate if invalid
            _uiState.update { it.copy(rate = null) }
            return
        }

        _uiState.update { it.copy(rateLoading = true) }

        rateFetchJob?.cancel()
        rateFetchJob = launchOnIo {
            exolixRepository.getRate(
                coinFrom = fromCurrency.coin,
                networkFrom = fromCurrency.networkName,
                coinTo = toCurrency.coin,
                networkTo = toCurrency.networkName,
                amount = amount.toString(),
                rateType = if (_uiState.value.fixedRate) Exolix.Rate.RateType.FIXED else Exolix.Rate.RateType.FLOATING,
            ).onSuccess { rate ->
                _uiState.update {
                    it.copy(
                        rate = rate,
                        rateLoading = false,
                    )
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(rateLoading = false) }
                showErrorDialog(exception) // TODO better message
            }
        }
    }

    data class UiState(
        val amountValue: String = "",

        val selectedCurrency: CurrencyDto? = null, // TODO private?

        val loadingDefaultCurrency: Boolean = false,
        val loadingDefaultCurrencyError: Boolean = false,
        val selectedToCurrency: Exolix.Currency? = null,
        val selectedToNetwork: Exolix.Network? = null,

        val fixedRate: Boolean = false,
        val rate: Exolix.Rate? = null,
        val rateLoading: Boolean = false,
    ) {
        val amount: BigDecimal?
            get() = runCatching { amountValue.toBigDecimal() }.getOrNull()
        val isAmountValid: Boolean
            get() = amount != null

        val amountError: Boolean
            get() = rate != null && amount != null && (amount!! < rate.minAmount || amount!! > rate.maxAmount)

        val fromCurrency: CurrencyDto?
            get() = TARI_CURRENCY // TODO direction!!

        val toCurrency: CurrencyDto?
            get() = selectedCurrency // TODO direction!!

        val TARI_CURRENCY = CurrencyDto(
            currency = Exolix.Currency(
//    code = "XTM",
                code = "ETH", // TODO uncomment!!!
                name = "Tari",
                icon = "https://exolix.com/icons/networks/Tari_1755361659372.png",
                notes = "",
            ),
            network = Exolix.Network(
                name = "Tari",
//    network = "Tari",
                network = "ETH", // TODO uncomment!!!
                icon = "https://exolix.com/icons/networks/Tari_1755361659372.png",
                isDefault = true,
                memoNeeded = false,
                precision = 10,
            ),
        )
    }
}