package com.tari.android.wallet.ui.fragment.settings.logs.logs

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.ClipboardArgs
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.DebugLog
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.LogViewHolderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class LogsViewModel : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var bugReportingService: BugReportingService

    val filters = MutableLiveData<MutableList<LogFilters>>(mutableListOf())

    val logs = MutableLiveData<MutableList<LogViewHolderItem>>()

    val filteredLogs = MediatorLiveData<MutableList<LogViewHolderItem>>()

    init {
        component.inject(this)

        filteredLogs.addSource(filters) { filter() }
        filteredLogs.addSource(logs) { filter() }
    }

    fun initWithFile(file: File?) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val lines = file?.inputStream()?.bufferedReader()?.readLines()?.toMutableList() ?: return@launch
            logs.postValue(lines.map { LogViewHolderItem(DebugLog(it)) }.reversed().toMutableList())
        } catch (e: Throwable) {
            val errorArgs = ErrorDialogArgs(
                resourceManager.getString(R.string.common_error_title),
                resourceManager.getString(R.string.debug_logs_cant_open_file),
            ) {
                _backPressed.postValue(Unit)
            }
            _modularDialog.postValue(errorArgs.getModular(resourceManager))
            logger.e(e, "Out of memory on reading big log file")
        }
    }

    fun showFilters() {
        val currentFilters = filters.value!!
        val filterModules =
            LogFilters.values().map { LogCheckedModule(it, CheckedModule(resourceManager.getString(it.title), currentFilters.contains(it))) }
        val modules = mutableListOf<IDialogModule>()
        modules.add(HeadModule(resourceManager.getString(R.string.debug_log_filter_title)))
        modules.add(SpaceModule(8))
        modules.addAll(filterModules)
        modules.add(SpaceModule(12))
        modules.add(ButtonModule(resourceManager.getString(R.string.debug_log_filter_apply), ButtonStyle.Normal) {
            _dismissDialog.postValue(Unit)
            filters.postValue(filterModules.filter { it.checkedModule.isChecked }.map { it.logFilter }.toMutableList())
        })
        modules.add(ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close))

        val modularDialogArgs = ModularDialogArgs(DialogArgs(), modules)
        _modularDialog.postValue(modularDialogArgs)
    }

    fun copyToClipboard(item: LogViewHolderItem) {
        _copyToClipboard.postValue(
            ClipboardArgs(
                resourceManager.getString(R.string.debug_logs_title),
                item.log.line,
                resourceManager.getString(R.string.debug_logs_clipboard_text)
            )
        )
    }

    private fun filter() {
        val logs = logs.value ?: return
        val filters = filters.value ?: return

        var filteredLogs = logs
        if (filters.isNotEmpty()) {
            filteredLogs = logs.filter { item -> filters.any { it.isMatch(item.log) } }.toMutableList()
        }

        this.filteredLogs.postValue(filteredLogs)
    }
}