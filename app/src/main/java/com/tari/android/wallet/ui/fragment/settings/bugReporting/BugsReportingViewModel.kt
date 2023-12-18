package com.tari.android.wallet.ui.fragment.settings.bugReporting

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import kotlinx.coroutines.launch
import javax.inject.Inject

class BugsReportingViewModel : CommonViewModel() {

    @Inject
    lateinit var bugReportingService: BugReportingService

    init {
        component.inject(this)
    }

    fun send(name: String, email: String, bugDescription: String) = viewModelScope.launch {
        try {
            bugReportingService.share(name, email, bugDescription)
            backPressed.postValue(Unit)
        } catch (e: Exception) {
            val args = ModularDialogArgs(
                DialogArgs(),
                listOf(
                    HeadModule(resourceManager.getString(R.string.common_error_title)),
                    BodyModule(resourceManager.getString(R.string.common_unknown_error)),
                    ButtonModule(resourceManager.getString(R.string.common_ok), ButtonStyle.Close)
                )
            )
            modularDialog.postValue(args)
        }
    }
}

