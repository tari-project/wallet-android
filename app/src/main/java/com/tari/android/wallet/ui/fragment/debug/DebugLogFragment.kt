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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentDebugLogBinding
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.fragment.debug.adapter.LogFileSpinnerAdapter
import com.tari.android.wallet.ui.fragment.debug.adapter.LogListAdapter
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
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

    @Inject
    lateinit var bugReportingService: BugReportingService

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

    private lateinit var ui: FragmentDebugLogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentDebugLogBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        setupUI()
    }

    private fun setupUI() {
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
        ui.scrollToTopButton.temporarilyDisableClick()
        ui.recyclerView.scrollToPosition(0)
    }

    private fun onScrollToBottomButtonClicked() {
        ui.scrollToBottomButton.temporarilyDisableClick()
        ui.recyclerView.scrollToPosition(selectedLogFileLines.lastIndex)
    }

    private fun showShareLogFilesDialog() {
        ui.shareButton.temporarilyDisableClick()
        AlertDialog.Builder(context ?: return)
            .setMessage(string(debug_log_share_dialog_content))
            .setCancelable(false)
            .setPositiveButton(string(common_confirm)) { dialog, _ ->
                dialog.cancel()
                shareBugReport()
            }
            // negative button text and action
            .setNegativeButton(string(exit)) { dialog, _ -> dialog.cancel() }
            .setTitle(string(debug_log_share_dialog_title))
            .create()
            .show()
    }

    private fun shareBugReport() {
        val mContext = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bugReportingService.shareBugReport(mContext)
            } catch (e: BugReportingService.BugReportFileSizeLimitExceededException) {
                withContext(Dispatchers.Main) {
                    showBugReportFileSizeExceededDialog()
                }
            }
        }
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

    private fun showBugReportFileSizeExceededDialog() {
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

}
