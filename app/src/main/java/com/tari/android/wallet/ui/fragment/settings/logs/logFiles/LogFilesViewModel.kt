package com.tari.android.wallet.ui.fragment.settings.logs.logFiles

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.logs.logFiles.adapter.LogFileViewHolderItem
import com.tari.android.wallet.application.walletManager.WalletFileUtil
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

class LogFilesViewModel : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    val logFiles = MutableLiveData<MutableList<CommonViewHolderItem>>()

    val goNext = SingleLiveEvent<File>()

    init {
        component.inject(this)

        val files = WalletFileUtil.getLogFilesFromDirectory(walletConfig.getWalletLogFilesDirPath()).toMutableList()
        val wholeList = files.map { listOf(LogFileViewHolderItem(getFileName(it), it) { goNext.postValue(it.file) }, DividerViewHolderItem()) }
            .flatten()
            .toMutableList()
        logFiles.postValue(wholeList)
    }

    private fun getFileName(file: File): String = file.name + " - " + getReadableFileSize(file.length())

    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(bytesInKilo)).toInt()
        return DecimalFormat("#,##0.00").format(size / bytesInKilo.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    companion object {
        const val bytesInKilo = 1024.00
    }
}