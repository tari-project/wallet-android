package com.tari.android.wallet.ui.screen.settings.dataCollection

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class DataCollectionViewModel : CommonViewModel() {

    @Inject
    lateinit var sentryPrefRepository: SentryPrefRepository

    val state: MutableLiveData<Boolean> = MutableLiveData()

    init {
        component.inject(this)
        state.value = sentryPrefRepository.isEnabled == true
    }

    fun updateState(state: Boolean) {
        sentryPrefRepository.isEnabled = state
        this.state.value = state
    }
}