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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentDebugLogBinding
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.string
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
internal class DebugLogFragment : Fragment(), AdapterView.OnItemSelectedListener {

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

    private var _ui: FragmentDebugLogBinding? = null
    private val ui get() = _ui!!

    private val numberOfLogsFilesToShare = 2
    private val maxLogZipFileSizeBytes = 25 * 1024 * 1024

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentDebugLogBinding.inflate(inflater, container, false).also { _ui = it }.root

    override fun onDestroyView() {
        super.onDestroyView()
        ui.recyclerView.layoutManager = null
        ui.recyclerView.adapter = null
        _ui = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DebugLogFragmentVisitor.visit(this)
        setupUi()
    }

    private fun setupUi() {
        // read log files
        logFiles = WalletUtil.getLogFilesFromDirectory(logFilesDirPath)

        // initialize recycler view
        recyclerViewLayoutManager = LinearLayoutManager(activity)
        recyclerViewAdapter = LogListAdapter(selectedLogFileLines)
        spinnerAdapter = LogFileSpinnerAdapter(context!!, logFiles)
        ui.apply {
            recyclerView.layoutManager = recyclerViewLayoutManager
            recyclerView.adapter = recyclerViewAdapter
            fileSpinner.onItemSelectedListener = this@DebugLogFragment
            fileSpinner.adapter = spinnerAdapter
            scrollToTopButton.setOnClickListener { onScrollToTopButtonClicked() }
            scrollToBottomButton.setOnClickListener { onScrollToBottomButtonClicked() }
            shareButton.setOnClickListener { showShareLogFilesDialog() }
        }
    }

    private fun onScrollToTopButtonClicked() {
        UiUtil.temporarilyDisableClick(ui.scrollToTopButton)
        ui.recyclerView.scrollToPosition(0)
    }

    private fun onScrollToBottomButtonClicked() {
        UiUtil.temporarilyDisableClick(ui.scrollToBottomButton)
        ui.recyclerView.scrollToPosition(selectedLogFileLines.lastIndex)
    }

    private fun showShareLogFilesDialog() {
        UiUtil.temporarilyDisableClick(ui.shareButton)
        AlertDialog.Builder(context ?: return)
            .setMessage(string(debug_log_share_dialog_content))
            .setCancelable(false)
            .setPositiveButton(string(common_confirm)) { dialog, _ ->
                dialog.cancel()
                Thread(this::zipAndEmailLogFiles).start()
            }
            // negative button text and action
            .setNegativeButton(string(exit)) { dialog, _ -> dialog.cancel() }
            .setTitle(string(debug_log_share_dialog_title))
            .create()
            .show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // no-op
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        ui.recyclerView.scrollToPosition(0)
        selectedLogFile = logFiles[position]
        updateLogLines()
    }

    private fun updateLogLines() {
        ui.recyclerView.alpha = 0f
        selectedLogFileLines.clear()
        val inputStream: InputStream = selectedLogFile.inputStream()
        inputStream.bufferedReader()
            .useLines { lines -> lines.forEach { selectedLogFileLines.add(it) } }
        recyclerViewAdapter.notifyDataSetChanged()
        val anim = ObjectAnimator.ofFloat(ui.recyclerView, "alpha", 0f, 1f)
        anim.duration = Constants.UI.mediumDurationMs
        anim.startDelay = Constants.UI.shortDurationMs
        anim.start()
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
        val dialog = dialogBuilder.setMessage(
            string(debug_log_file_size_limit_exceeded_dialog_content)
        )
            .setCancelable(false)
            .setPositiveButton(string(common_ok)) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(string(debug_log_file_size_limit_exceeded_dialog_title))
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
            arrayOf(string(ffi_admin_email_address))
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
                string(common_share)
            )
        )
    }

    private object DebugLogFragmentVisitor {
        internal fun visit(fragment: DebugLogFragment) {
            fragment.requireActivity().appComponent.inject(fragment)
        }
    }

}
