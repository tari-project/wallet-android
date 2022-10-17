package com.tari.android.wallet.ui.fragment.settings.logs.logs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
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

    val logs = MutableLiveData<MutableList<LogViewHolderItem>>()

    init {
        component.inject(this)
    }

    fun initWithFile(file: File?) = viewModelScope.launch(Dispatchers.IO) {
        val lines = file?.inputStream()?.bufferedReader()?.readLines()?.toMutableList() ?: return@launch
        logs.postValue(lines.map { LogViewHolderItem(it) }.toMutableList())
    }
}