package com.tari.android.wallet.ui.fragment.settings.screenRecording

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.loadingSwitch.TariLoadingSwitchState
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenRecordingSettingsViewModel : CommonViewModel() {

    private val _switchState = MutableStateFlow(TariLoadingSwitchState(isChecked = tariSettingsSharedRepository.screenRecordingTurnedOn))
    val switchState = _switchState.asStateFlow()

    init {
        component.inject(this)
    }

    fun toggleScreenRecordingEnable(isChecked: Boolean) {
        if (!isChecked) {
            turnSwitcher(false)
        } else {
            _switchState.update { it.startLoading() }
            modularDialog.value = ConfirmDialogArgs(
                resourceManager.getString(R.string.screen_recording_button_confirmation_title),
                resourceManager.getString(R.string.screen_recording_button_confirmation_description),
                onConfirm = { turnSwitcher(true) },
                onCancel = { _switchState.update { it.stopLoading() } },
                onDismiss = { _switchState.update { it.stopLoading() } }
            ).getModular(resourceManager)
        }
    }

    private fun turnSwitcher(isTurnedOn: Boolean) {
        tariSettingsSharedRepository.screenRecordingTurnedOn = isTurnedOn
        _switchState.update { TariLoadingSwitchState(isTurnedOn, false) }
        dismissDialog.postValue(Unit)
    }
}