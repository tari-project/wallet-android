package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletModel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class CreateWalletViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        viewModelScope.launch(Dispatchers.IO) {
            EventBus.torProxyState.publishSubject.asFlow().collect {
                _uiState.value = _uiState.value.copy(torState = it)
            }
        }
    }

    fun onContinueButtonClick() {
        corePrefRepository.onboardingCompleted = true
        corePrefRepository.onboardingAuthSetupStarted = true
    }
}