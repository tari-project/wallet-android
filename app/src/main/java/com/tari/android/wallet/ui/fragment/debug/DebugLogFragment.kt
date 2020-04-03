/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.debug

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindString
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.debug.adapter.LogFileSpinnerAdapter
import com.tari.android.wallet.ui.fragment.debug.adapter.LogListAdapter
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Debug: show logs from file.
 *
 * @author The Tari Development Team
 */
internal class DebugLogFragment : BaseFragment(), AdapterView.OnItemSelectedListener {

    override val contentViewId = R.layout.fragment_debug_log

    @BindView(R.id.debug_log_file_spinner)
    lateinit var spinner: Spinner
    @BindView(R.id.debug_log_recycler_view)
    lateinit var recyclerView: RecyclerView

    @BindString(R.string.ffi_admin_email_address)
    lateinit var ffiAdminEmailAddress: String
    @BindString(R.string.common_share)
    lateinit var sharePrompt: String
    @BindString(R.string.debug_log_file_size_limit_exceeded_dialog_title)
    lateinit var fileSizeExceededDialogTitle: String
    @BindString(R.string.debug_log_file_size_limit_exceeded_dialog_content)
    lateinit var fileSizeExceededDialogContent: String

    @Inject
    @Named(WalletModule.FieldName.walletLogFilesDirPath)
    lateinit var logFilesDirPath: String
    @Inject
    @Named(WalletModule.FieldName.walletLogFilePath)
    lateinit var logFilePath: String

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    /**
     * Log file related vars.
     */
    private lateinit var logFiles: List<File>
    private lateinit var selectedLogFile: File
    private val selectedLogFileLines = mutableListOf<String>()

    /**
     * List, adapter & layout manager.
     */
    private lateinit var spinnerAdapter: LogFileSpinnerAdapter
    private lateinit var recyclerViewAdapter: LogListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    private val numberOfLogsFilesToShare = 2
    private val maxLogZipFileSizeBytes = 25 * 1024 * 1024

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // read log files
        logFiles = WalletUtil.getLogFilesFromDirectory(logFilesDirPath)

        // initialize recycler view
        recyclerViewLayoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter = LogListAdapter(selectedLogFileLines)
        recyclerView.adapter = recyclerViewAdapter
        spinner.onItemSelectedListener = this

        spinnerAdapter = LogFileSpinnerAdapter(context!!, logFiles)
        spinner.adapter = spinnerAdapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null
        super.onDestroyView()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // no-op
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        recyclerView.scrollToPosition(0)
        selectedLogFile = logFiles[position]
        updateLogLines()
    }

    private fun updateLogLines() {
        recyclerView.alpha = 0f
        selectedLogFileLines.clear()
        val inputStream: InputStream = selectedLogFile.inputStream()
        inputStream.bufferedReader()
            .useLines { lines -> lines.forEach { selectedLogFileLines.add(it) } }
        recyclerViewAdapter.notifyDataSetChanged()
        val anim = ObjectAnimator.ofFloat(recyclerView, "alpha", 0f, 1f)
        anim.duration = Constants.UI.mediumDurationMs
        anim.startDelay = Constants.UI.shortDurationMs
        anim.start()
    }

    @OnClick(R.id.debug_log_btn_scroll_to_top)
    fun onScrollToTopButtonClicked(button: ImageButton) {
        UiUtil.temporarilyDisableClick(button)
        recyclerView.scrollToPosition(0)
    }

    @OnClick(R.id.debug_log_btn_scroll_to_bottom)
    fun onScrollToTopBottomClicked(button: ImageButton) {
        UiUtil.temporarilyDisableClick(button)
        recyclerView.scrollToPosition(
            selectedLogFileLines.size - 1
        )
    }

    @OnClick(R.id.debug_log_btn_share)
    fun showShareLogFilesDialog(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val dialogBuilder = AlertDialog.Builder(context ?: return)
        val dialog = dialogBuilder.setMessage(getString(R.string.debug_log_share_dialog_content))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.common_confirm)) { dialog, _ ->
                dialog.cancel()
                Thread {
                    zipAndEmailLogFiles()
                }.start()
            }
            // negative button text and action
            .setNegativeButton(getString(R.string.exit)) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(getString(R.string.debug_log_share_dialog_title))
            .create()
        dialog.show()
    }

    private fun zipAndEmailLogFiles() {
        // delete if zipped file exists
        val publicKeyHex = sharedPrefsWrapper.publicKeyHexString
        val zipFile = File(
            logFilesDirPath,
            "ffi_logs_${publicKeyHex}.zip"
        )
        if (zipFile.exists()) {
            zipFile.delete()
        }
        val fileOut = FileOutputStream(zipFile)
        // zip!
        val allLogFiles = WalletUtil.getLogFilesFromDirectory(logFilesDirPath)
        val logFilesToShare = allLogFiles.take(numberOfLogsFilesToShare)
        ZipOutputStream(BufferedOutputStream(fileOut)).use { out ->
            for (file in logFilesToShare) {
                FileInputStream(file).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        val entry = ZipEntry(file.name)
                        out.putNextEntry(entry)
                        origin.copyTo(out, 1024)
                        origin.close()
                    }
                    fi.close()
                }
            }
            out.closeEntry()
            out.close()
        }
        // check zip file size
        if (zipFile.length() > maxLogZipFileSizeBytes) {
            zipFile.delete()
            Handler().post {
                showFileSizeExceededDialog()
            }
        } else {
            shareLogZipFile(zipFile)
        }
    }

    private fun showFileSizeExceededDialog() {
        val dialogBuilder = AlertDialog.Builder(context ?: return)
        val dialog = dialogBuilder.setMessage(fileSizeExceededDialogContent)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.common_ok)) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(fileSizeExceededDialogTitle)
            .create()
        dialog.show()
    }

    private fun shareLogZipFile(zipFile: File) {
        val mContext = context ?: return
        // file is zipped, create the intent
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        val intent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        val zipFileUri = FileProvider.getUriForFile(
            mContext,
            "com.tari.android.wallet.files",
            zipFile
        )
        intent.putExtra(Intent.EXTRA_STREAM, zipFileUri)
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Public Key:\n" + sharedPrefsWrapper.publicKeyHexString + "\n\n"
                    + "Emoji Id:\n" + sharedPrefsWrapper.emojiId
        )
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf(ffiAdminEmailAddress)
        )
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            "FFI Log Files"
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.selector = emailIntent
        mContext.startActivity(
            Intent.createChooser(
                intent,
                sharePrompt
            )
        )
    }

}