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
package com.tari.android.wallet.ui.fragment.debug.debugLog

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentDebugLogBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.fragment.debug.debugLog.adapter.LogFileSpinnerAdapter
import com.tari.android.wallet.ui.fragment.debug.debugLog.adapter.LogListAdapter
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugLogFragment : CommonFragment<FragmentDebugLogBinding, DebugLogViewModel>(), AdapterView.OnItemSelectedListener {

    private lateinit var recyclerViewAdapter: LogListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentDebugLogBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: DebugLogViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeUI()
    }

    private fun setupUI() = with(ui) {
        recyclerViewAdapter = LogListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recyclerViewAdapter
        fileSpinner.onItemSelectedListener = this@DebugLogFragment
        scrollToTopButton.setOnClickListener { onScrollToTopButtonClicked() }
        scrollToBottomButton.setOnClickListener { onScrollToBottomButtonClicked() }
        shareButton.setOnClickListener { showShareLogFilesDialog() }
    }

    private fun observeUI() = with(viewModel) {
        observe(selectedLogFileLines) { updateLogFileLines(it) }

        observe(logFiles) { ui.fileSpinner.adapter = LogFileSpinnerAdapter(requireContext(), it) }
    }

    private fun updateLogFileLines(lines: MutableList<String>) {
        ui.recyclerView.alpha = 0f
        recyclerViewAdapter.logLines.clear()
        recyclerViewAdapter.logLines.addAll(lines)
        recyclerViewAdapter.notifyDataSetChanged()
        ObjectAnimator.ofFloat(ui.recyclerView, "alpha", 0f, 1f).apply {
            duration = Constants.UI.mediumDurationMs
            startDelay = Constants.UI.shortDurationMs
            start()
        }
    }

    private fun onScrollToTopButtonClicked() {
        ui.scrollToTopButton.temporarilyDisableClick()
        ui.recyclerView.scrollToPosition(0)
    }

    private fun onScrollToBottomButtonClicked() {
        ui.scrollToBottomButton.temporarilyDisableClick()
        ui.recyclerView.scrollToPosition(recyclerViewAdapter.logLines.lastIndex)
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
        val mContext = requireContext()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                viewModel.bugReportingService.shareBugReport(mContext)
            } catch (e: BugReportingService.BugReportFileSizeLimitExceededException) {
                withContext(Dispatchers.Main) {
                    showBugReportFileSizeExceededDialog()
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        ui.recyclerView.scrollToPosition(0)
        viewModel.selectFile(position)
    }

    private fun showBugReportFileSizeExceededDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(string(debug_log_file_size_limit_exceeded_dialog_content))
            .setCancelable(false)
            .setPositiveButton(string(common_ok)) { dialog, _ -> dialog.cancel() }
            .setTitle(string(debug_log_file_size_limit_exceeded_dialog_title))
            .create()
            .show()
    }
}