package com.tari.android.wallet.ui.screen.settings.logs.logs

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.screen.debug.activity.DebugActivity
import com.tari.android.wallet.ui.screen.settings.logs.logs.adapter.DebugLog
import com.tari.android.wallet.ui.screen.settings.logs.logs.adapter.LogViewHolderItem
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogLevelCheckedModule
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogLevelFilters
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogSourceCheckedModule
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogSourceFilters
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.switchToMain
import java.io.File

class LogsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    private val logLevelFilters = MutableLiveData<MutableList<LogLevelFilters>>(mutableListOf())
    private val logSourceFilters = MutableLiveData<MutableList<LogSourceFilters>>(mutableListOf())
    private val logs = MutableLiveData<MutableList<LogViewHolderItem>>()

    val file = savedState.get<File>(DebugActivity.LOG_FILE)

    val filteredLogs = MediatorLiveData<MutableList<LogViewHolderItem>>()

    init {
        component.inject(this)

        filteredLogs.addSource(logLevelFilters) { filter() }
        filteredLogs.addSource(logSourceFilters) { filter() }
        filteredLogs.addSource(logs) { filter() }

        launchOnIo {
            try {
                val lines = file!!.inputStream().bufferedReader().readLines().toMutableList()
                logs.postValue(lines.map { LogViewHolderItem(DebugLog(it)) }.reversed().toMutableList())
            } catch (e: Throwable) {
                switchToMain {
                    showSimpleDialog(
                        title = resourceManager.getString(R.string.common_error_title),
                        description = resourceManager.getString(R.string.debug_logs_cant_open_file),
                        onClose = { backPressed.postValue(Unit) },
                    )
                }
                logger.i("Error reading log file: ${e.message}")
            }
        }
    }

    fun showFilters() {
        val currentLevelFilters = logLevelFilters.value!!
        val currentSourceFilters = logSourceFilters.value!!
        val levelFilterModules = LogLevelFilters.entries
            .map { LogLevelCheckedModule(it, CheckedModule(resourceManager.getString(it.title), currentLevelFilters.contains(it))) }
        val sourceFiltersModules = LogSourceFilters.entries
            .map { LogSourceCheckedModule(it, CheckedModule(resourceManager.getString(it.title), currentSourceFilters.contains(it))) }
        val modules = mutableListOf<IDialogModule>()
        modules.add(HeadModule(resourceManager.getString(R.string.debug_log_filter_title)))
        modules.add(SpaceModule(8))
        modules.addAll(levelFilterModules)
        modules.addAll(sourceFiltersModules)
        modules.add(SpaceModule(12))
        modules.add(ButtonModule(resourceManager.getString(R.string.debug_log_filter_apply), ButtonStyle.Normal) {
            hideDialog()
            logLevelFilters.postValue(levelFilterModules.filter { it.checkedModule.isChecked }.map { it.logFilter }.toMutableList())
            logSourceFilters.postValue(sourceFiltersModules.filter { it.checkedModule.isChecked }.map { it.logFilter }.toMutableList())
        })
        modules.add(ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close))

        val modularDialogArgs = ModularDialogArgs(DialogArgs(), modules)
        showModularDialog(modularDialogArgs)
    }

    fun copyToClipboard(item: LogViewHolderItem) {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.debug_logs_title),
            clipText = item.log.line,
            toastMessage = resourceManager.getString(R.string.debug_logs_clipboard_text),
        )
    }

    private fun filter() {
        val logs = logs.value ?: return
        val logLevelFilters = logLevelFilters.value ?: return
        val sourceLevelFilters = logSourceFilters.value ?: return

        var filteredLogs = logs
        if (logLevelFilters.isNotEmpty()) {
            filteredLogs = filteredLogs.filter { item -> logLevelFilters.any { it.isMatch(item.log.auroraDebugLog ?: item.log) } }.toMutableList()
        }

        if (sourceLevelFilters.isNotEmpty()) {
            filteredLogs = filteredLogs.filter { item -> sourceLevelFilters.any { it.isMatch(item.log) } }.toMutableList()
        }

        this.filteredLogs.postValue(filteredLogs)
    }
}