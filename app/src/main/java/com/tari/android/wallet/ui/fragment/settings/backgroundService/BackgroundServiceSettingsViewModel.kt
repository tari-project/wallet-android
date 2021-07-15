package com.tari.android.wallet.ui.fragment.settings.backgroundService

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.component.loadingSwitch.LoadingSwitchState
import com.tari.android.wallet.ui.viewModel.CommonViewModel

class BackgroundServiceSettingsViewModel: CommonViewModel() {

    private val _switchState = MutableLiveData<LoadingSwitchState>()
    val switchState : LiveData<LoadingSwitchState> = _switchState

    fun toggleBackgroundServiceEnable(isChecked: Boolean) {

    }
}