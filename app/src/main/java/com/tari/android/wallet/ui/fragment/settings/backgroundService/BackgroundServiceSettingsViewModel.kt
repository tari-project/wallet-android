package com.tari.android.wallet.ui.fragment.settings.backgroundService

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.component.loadingSwitch.LoadingSwitchState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import javax.inject.Inject

class BackgroundServiceSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    @Inject
    lateinit var resourceManager: ResourceManager

    private val _switchState = MutableLiveData<LoadingSwitchState>()
    val switchState: LiveData<LoadingSwitchState> = _switchState

    init {
        component?.inject(this)

        _switchState.postValue(LoadingSwitchState(sharedPrefsRepository.backgroundServiceTurnedOn, false))
    }

    fun toggleBackgroundServiceEnable(isChecked: Boolean) {
        if (isChecked) {
            turnSwitcher(true)
        } else {
            _switchState.value = LoadingSwitchState(isChecked = true, isLoading = true)
            _confirmDialog.value = ConfirmDialogArgs(
                resourceManager.getString(R.string.background_service_button_confirmation_title),
                resourceManager.getString(R.string.background_service_button_confirmation_description),
                onConfirm = { turnSwitcher(false) },
                onCancel = { _switchState.value = LoadingSwitchState(isChecked = true, isLoading = false) },
                onDismiss = { _switchState.value = LoadingSwitchState(isChecked = _switchState.value!!.isChecked, isLoading = false) })
        }
    }

    private fun turnSwitcher(isTurnedOn: Boolean) {
        sharedPrefsRepository.backgroundServiceTurnedOn = isTurnedOn
        _switchState.value = LoadingSwitchState(isTurnedOn, false)
    }
}