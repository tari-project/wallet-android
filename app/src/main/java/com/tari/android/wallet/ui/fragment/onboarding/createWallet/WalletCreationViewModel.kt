package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.infrastructure.yat.YatService
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.model.yat.EmojiId
import com.tari.android.wallet.ui.component.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletCreationViewModel(private val service: YatService) : ViewModel() {

    private val _state = SingleLiveEvent<WalletCreationState>()
    val state: LiveData<WalletCreationState> get() = _state

    init {
        _state.value = InitialState
    }

    fun onStart(action: () -> Unit) {
        _state.value = SearchingForInitialYatState
        action()
    }

    fun onStartAgain(action: () -> Unit) {
        action()
    }

    fun handleYatState(dto: YatAdapter.YatIntegrationStateDto) {
        when (dto.state) {
            YatAdapter.YatIntegrationState.None -> Unit
            YatAdapter.YatIntegrationState.Complete -> {
                _state.value = InitialYatFoundState(EmojiId(dto.yat.orEmpty()))
            }
            YatAdapter.YatIntegrationState.Failed -> {
                if (_state.value == SearchingForInitialYatState) {
                    _state.value = InitialYatSearchErrorState(Exception(dto.failureType?.name))
                }
            }
        }
    }

    fun onYatAcquireInitiated(yat: String) {
        viewModelScope.launch {
            _state.value = YatAcquiringState
            withContext(Dispatchers.IO) {
                service.saveYat(yat)
                _state.postValue(YatAcquiredState)
            }
        }
    }
}