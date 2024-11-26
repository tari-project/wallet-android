package com.tari.android.wallet.ui.screen.settings.bugReporting

import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.switchToMain
import javax.inject.Inject

class BugsReportingViewModel : CommonViewModel() {

    @Inject
    lateinit var bugReportingService: BugReportingService

    init {
        component.inject(this)
    }

    fun send(name: String, email: String, bugDescription: String) = launchOnIo {
        try {
            bugReportingService.share(name, email, bugDescription)
            backPressed.postValue(Unit)
            logger.i("Bug report sent: name=$name, email=$email, bugDescription=$bugDescription")
        } catch (e: Exception) {
            switchToMain {
                showSimpleDialog(
                    title = resourceManager.getString(R.string.common_error_title),
                    description = e.message?.let { resourceManager.getString(R.string.bugs_reporting_send_logs_error, it) }
                        ?: resourceManager.getString(R.string.common_unknown_error),
                    closeButtonTextRes = R.string.common_ok,
                )
            }
            logger.e("Error sending bug report: ${e.message}")
        }
    }
}
