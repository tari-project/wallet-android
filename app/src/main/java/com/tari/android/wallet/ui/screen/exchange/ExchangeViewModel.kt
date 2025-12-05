package com.tari.android.wallet.ui.screen.exchange

import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.ExchangeDirection
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.filterNumbers
import com.tari.android.wallet.util.extension.filterSingleDot
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.toMicroTari
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

    @Inject
    lateinit var balanceStateHandler: BalanceStateHandler

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            availableBalance = balanceStateHandler.balanceState.value.availableBalance,
        )
    )
    val uiState = _uiState.asStateFlow()

    private var rateFetchJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        loadCurrencies()
        collectFlow(balanceStateHandler.balanceState) { balanceState ->
            _uiState.update { it.copy(availableBalance = balanceState.availableBalance) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    fun onAmountChanged(amountValue: String) {
        val newAmount = amountValue.filterSingleDot().filterNumbers()
        if (newAmount == _uiState.value.amountValue) return

        _uiState.update {
            it.copy(
                amountValue = amountValue.filterSingleDot().filterNumbers(),
                lastEditedField = LastEditedField.SEND_AMOUNT,
            )
        }
        fetchRateIfValid()
    }

    fun onReceiveAmountChanged(receiveAmountValue: String) {
        val newAmount = receiveAmountValue.filterSingleDot().filterNumbers()
        if (newAmount == _uiState.value.receiveAmountValue) return

        _uiState.update {
            it.copy(
                receiveAmountValue = receiveAmountValue.filterSingleDot().filterNumbers(),
                lastEditedField = LastEditedField.RECEIVE_AMOUNT,
            )
        }
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
                },
                amountValue = uiState.value.receiveAmountValue,
                receiveAmountValue = uiState.value.amountValue,
                lastEditedField = when (it.lastEditedField) {
                    LastEditedField.SEND_AMOUNT -> LastEditedField.RECEIVE_AMOUNT
                    LastEditedField.RECEIVE_AMOUNT -> LastEditedField.SEND_AMOUNT
                },
            )
        }
        fetchRateIfValid()
    }

    fun onDestinationAddressChanged(address: String) {
        _uiState.update { it.copy(destinationAddress = address) }
    }

    override fun handleDeeplink(deeplink: DeepLink) {
        when (deeplink) {
            is DeepLink.Send -> {
                if (deeplink.walletAddress.isNotEmpty()) {
                    _uiState.update { it.copy(destinationAddress = deeplink.walletAddress) }
                }
            }

            is DeepLink.Raw -> {
                if (deeplink.value.isNotEmpty()) {
                    _uiState.update { it.copy(destinationAddress = deeplink.value) }
                }
            }

            else -> super.handleDeeplink(deeplink)
        }
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

        // Determine which amount to use based on last edited field
        val (amount, withdrawalAmount) = when (_uiState.value.lastEditedField) {
            LastEditedField.SEND_AMOUNT -> _uiState.value.amount to null
            LastEditedField.RECEIVE_AMOUNT -> null to _uiState.value.receiveAmount
        }

        if (amount == null && withdrawalAmount == null) error("Both amount and withdrawalAmount are null, but exchange button is not disabled")

        val request = ExchangeRequestData(
            selectedCurrency = _uiState.value.selectedCurrency ?: error("selectedCurrency is null, but exchange button is not disabled"),
            tariCurrency = _uiState.value.tariCurrency ?: error("tariCurrency is null, but exchange button is not disabled"),
            selectedAddress = _uiState.value.destinationAddress.takeIf { it.isNotBlank() },
            amount = amount,
            withdrawalAmount = withdrawalAmount,
            rateType = if (_uiState.value.fixedRate) Exolix.RateType.FIXED else Exolix.RateType.FLOATING,
            direction = _uiState.value.exchangeDirection,
        )

        tariNavigator.navigate(
            when (_uiState.value.exchangeDirection) {
                ExchangeDirection.SELL_TARI -> Navigation.Exchange.Review(request)
                ExchangeDirection.BUY_TARI -> Navigation.Exchange.SendFunds(request)
            }
        )
    }

    fun loadCurrencies() {
        _uiState.update { it.copy(loadingCurrencies = true, loadingCurrenciesError = false) }
        launchOnIo {
            val tariResult = exolixRepository.getTariCurrency()
            val selectedResult = exolixRepository.getCurrencies(page = 1, size = 1)

            if (tariResult.isSuccess && selectedResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        tariCurrency = tariResult.getOrThrow(),
                        selectedCurrency = selectedResult.getOrThrow().currencies.firstOrNull() ?: error("No currencies available"),
                        loadingCurrencies = false,
                        loadingCurrenciesError = false
                    )
                }

                fetchRateIfValid()
            } else {
                _uiState.update { it.copy(loadingCurrencies = false, loadingCurrenciesError = true) }
            }
        }
    }

    private fun fetchRateIfValid() {
        rateFetchJob?.cancel()

        val fromCurrency = _uiState.value.fromCurrency
        val toCurrency = _uiState.value.toCurrency

        // Determine which amount to use based on last edited field
        val (amount, withdrawalAmount) = when (_uiState.value.lastEditedField) {
            LastEditedField.SEND_AMOUNT -> _uiState.value.amount to null
            LastEditedField.RECEIVE_AMOUNT -> null to _uiState.value.receiveAmount
        }

        if (fromCurrency == null || toCurrency == null || (amount == null && withdrawalAmount == null)) {
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
                amount = amount?.toString(),
                withdrawalAmount = withdrawalAmount?.toString(),
                rateType = if (_uiState.value.fixedRate) Exolix.RateType.FIXED else Exolix.RateType.FLOATING,
            ).onSuccess { rate ->
                // Update the opposite field based on which was edited
                _uiState.update {
                    when (it.lastEditedField) {
                        LastEditedField.SEND_AMOUNT -> {
                            // Send was edited, update receive with toAmount from API
                            it.copy(
                                rate = rate,
                                rateLoading = false,
                                receiveAmountValue = rate.toAmount.toString()
                            )
                        }

                        LastEditedField.RECEIVE_AMOUNT -> {
                            // Receive was edited, update send with amount from API
                            it.copy(
                                rate = rate,
                                rateLoading = false,
                                amountValue = rate.fromAmount.toString()
                            )
                        }
                    }
                }
                startAutoRefresh()
            }.onFailure { error ->
                _uiState.update { it.copy(rateLoading = false) }
                logger.d("Failed to fetch exchange rate: ${error.message}")
                showSimpleDialog(
                    title = resourceManager.getString(R.string.exchange_rate_error_title),
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

    enum class LastEditedField {
        SEND_AMOUNT,
        RECEIVE_AMOUNT,
    }

    data class UiState(
        val amountValue: String = "0.1",
        val receiveAmountValue: String = "",

        val selectedCurrency: CurrencyDto? = null,
        val tariCurrency: CurrencyDto? = null,
        val exchangeDirection: ExchangeDirection = ExchangeDirection.BUY_TARI,
        val destinationAddress: String = "",

        val loadingCurrencies: Boolean = false,
        val loadingCurrenciesError: Boolean = false,

        val fixedRate: Boolean = false,
        val rate: Exolix.Rate? = null,
        val rateLoading: Boolean = false,
        val autoRefreshActive: Boolean = false,

        val availableBalance: MicroTari,
        val lastEditedField: LastEditedField = LastEditedField.SEND_AMOUNT,
    ) {
        val amount: BigDecimal?
            get() = runCatching { amountValue.toBigDecimal() }.getOrNull()

        val receiveAmount: BigDecimal?
            get() = runCatching { receiveAmountValue.toBigDecimal() }.getOrNull()

        val rateAmountError: Boolean
            get() = rate != null && amount != null && (amount!! < rate.minAmount || amount!! > rate.maxAmount)

        val availableBalanceError: Boolean
            get() = exchangeDirection == ExchangeDirection.SELL_TARI &&
                    amount != null &&
                    amount!!.toMicroTari() > availableBalance

        val amountErrorMessage: Int?
            get() = when {
                amount != null && amount!! <= BigDecimal.ZERO -> R.string.send_amount_field_error
                rateAmountError -> R.string.exchange_amount_outside_rate_limits
                availableBalanceError -> R.string.exchange_insufficient_funds_error
                else -> null
            }

        val fromCurrency: CurrencyDto?
            get() = if (exchangeDirection == ExchangeDirection.BUY_TARI) selectedCurrency else tariCurrency

        val toCurrency: CurrencyDto?
            get() = if (exchangeDirection == ExchangeDirection.BUY_TARI) tariCurrency else selectedCurrency

        val destinationAddressError: Boolean
            get() {
                if (exchangeDirection != ExchangeDirection.SELL_TARI || destinationAddress.isBlank()) return false
                val addressRegex = selectedCurrency?.network?.addressRegex ?: return false
                return !destinationAddress.matches(Regex(addressRegex))
            }

        val exchangeButtonEnabled: Boolean
            get() {
                // At least one amount field must be filled
                val hasValidAmount = (amount != null && amount!! > BigDecimal.ZERO) ||
                        (receiveAmount != null && receiveAmount!! > BigDecimal.ZERO)

                val baseCondition = fromCurrency != null &&
                        toCurrency != null &&
                        hasValidAmount &&
                        !rateAmountError &&
                        !availableBalanceError &&
                        !rateLoading
                return if (exchangeDirection == ExchangeDirection.SELL_TARI) {
                    baseCondition && destinationAddress.isNotBlank() && !destinationAddressError
                } else {
                    baseCondition
                }
            }
    }
}