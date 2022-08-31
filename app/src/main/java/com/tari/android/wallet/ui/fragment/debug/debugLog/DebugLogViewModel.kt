package com.tari.android.wallet.ui.fragment.debug.debugLog

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.WalletUtil
import java.io.File
import javax.inject.Inject

class DebugLogViewModel : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var bugReportingService: BugReportingService

    val logFiles = MutableLiveData<List<File>>()
    val selectedLogFileLines = MutableLiveData<MutableList<String>>()

    init {
        component.inject(this)

        logFiles.postValue(WalletUtil.getLogFilesFromDirectory(walletConfig.getWalletLogFilesDirPath()))
    }

    fun selectFile(position: Int) {
        val lines = logFiles.value?.get(position)?.inputStream()?.bufferedReader()?.readLines()?.toMutableList() ?: return
        selectedLogFileLines.postValue(lines)
    }
}