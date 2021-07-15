package com.tari.android.wallet.ui.fragment.settings.backgroundService

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.component.loadingSwitch.LoadingSwitchState
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class BackgroundServiceSettingsViewModel : CommonViewModel() {

    init {
        component?.inject(this)
    }

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    private val _switchState = MutableLiveData<LoadingSwitchState>()
    val switchState: LiveData<LoadingSwitchState> = _switchState


    fun toggleBackgroundServiceEnable(isChecked: Boolean) {
        if (isChecked) {
            turnSwitcher(true)
        } else {
            // todo ask
        }
    }

    fun turnSwitcher(isTurnedOn: Boolean) {
        sharedPrefsRepository.backgroundServiceTurnedOn = isTurnedOn
    }
}