package com.tari.android.wallet.ui.fragment.settings.bugReporting

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class BugsReportingViewModel : CommonViewModel() {

    @Inject
    lateinit var bugReportingService: BugReportingService

    init {
        component.inject(this)
    }

    fun send(name: String, email: String, bugDescription: String) = viewModelScope.launch {
        bugReportingService.share(name, email, bugDescription)
        backPressed.postValue(Unit)
    }
}

