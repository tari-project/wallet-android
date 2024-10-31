package com.tari.android.wallet.ui.fragment.settings.logs

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class LogFilesManager : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    init {
        component.inject(this)

        EventBus.subscribe<Event.App.AppBackgrounded>(this) { manage() }
        EventBus.subscribe<Event.App.AppForegrounded>(this) { manage() }
    }

    private fun manage() = viewModelScope.launch {
        val files = walletConfig.getLogFiles()
        if (files.size > MAX_FILES) {
            files.sortedByDescending { it.lastModified() }.drop(10).forEach { fileToDelete ->
                runCatching { fileToDelete.delete() }
            }
        }
    }

    companion object {
        const val MAX_FILES = 10
    }
}