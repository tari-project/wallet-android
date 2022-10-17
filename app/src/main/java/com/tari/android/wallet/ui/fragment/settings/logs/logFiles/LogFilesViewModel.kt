package com.tari.android.wallet.ui.fragment.settings.logs.logFiles

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.logging.BugReportingService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.logs.logFiles.adapter.LogFileViewHolderItem
import com.tari.android.wallet.util.WalletUtil
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject

class LogFilesViewModel : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var bugReportingService: BugReportingService

    val logFiles = MutableLiveData<MutableList<CommonViewHolderItem>>()

    val goNext = SingleLiveEvent<File>()

    init {
        component.inject(this)

        val files = WalletUtil.getLogFilesFromDirectory(walletConfig.getWalletLogFilesDirPath()).toMutableList()
        files.add(File(walletConfig.getApplicationLogsFilePath()))
        val wholeList =
            files.map { listOf(LogFileViewHolderItem(getFileName(it), it) { goNext.postValue(it.file) }, DividerViewHolderItem()) }.flatten()
                .toMutableList()
        logFiles.postValue(wholeList)
    }

    private fun getFileName(file: File): String = file.name + " - " + getFileSizeStr(file)

    private fun getFileSizeStr(file: File): String {
        val formatter = DecimalFormat("#,##0.00").apply {
            roundingMode = RoundingMode.FLOOR
        }
        return formatter.format(file.length() / bytesInKilo / bytesInKilo) + " MB"
    }

    companion object {
        const val bytesInKilo = 1024.00
    }
}