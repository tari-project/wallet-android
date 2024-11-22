package com.tari.android.wallet.ui.screen.settings.backgroundService

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.loadingSwitch.TariLoadingSwitchState
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import javax.inject.Inject

class BackgroundServiceSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var serviceLauncher: WalletServiceLauncher

    private val _switchState = MutableLiveData<TariLoadingSwitchState>()
    val switchState: LiveData<TariLoadingSwitchState> = _switchState

    init {
        component.inject(this)

        _switchState.postValue(TariLoadingSwitchState(tariSettingsSharedRepository.backgroundServiceTurnedOn, false))
    }

    fun toggleBackgroundServiceEnable(isChecked: Boolean) {
        if (isChecked) {
            turnSwitcher(true)
        } else {
            _switchState.value = TariLoadingSwitchState(isChecked = true, isLoading = true)
            showModularDialog(
                ConfirmDialogArgs(
                    title = resourceManager.getString(R.string.background_service_button_confirmation_title),
                    description = resourceManager.getString(R.string.background_service_button_confirmation_description),
                    onConfirm = { turnSwitcher(false) },
                    onCancel = { _switchState.value = TariLoadingSwitchState(isChecked = true, isLoading = false) },
                    onDismiss = { _switchState.value = TariLoadingSwitchState(isChecked = _switchState.value!!.isChecked, isLoading = false) }
                ).getModular(resourceManager)
            )
        }
    }

    private fun turnSwitcher(isTurnedOn: Boolean) {
        tariSettingsSharedRepository.backgroundServiceTurnedOn = isTurnedOn
        _switchState.value = TariLoadingSwitchState(isTurnedOn, false)
        hideDialog()
        serviceLauncher.startIfWalletExists()
    }
}