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
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.fragment.debug.adapter.LogFileSpinnerAdapter
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.debug.adapter.LogListAdapter
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Debug: show logs from file.
 *
 * @author The Tari Development Team
 */
class DebugLogFragment : BaseFragment(), AdapterView.OnItemSelectedListener {

    override val contentViewId = R.layout.fragment_debug_log

    @BindView(R.id.debug_log_file_spinner)
    lateinit var spinner: Spinner
    @BindView(R.id.debug_log_recycler_view)
    lateinit var recyclerView: RecyclerView

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String
    @Inject
    @Named(WalletModule.FieldName.walletLogFilePath)
    internal lateinit var logFilePath: String

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // read log files
        logFiles = WalletUtil.getLogFilesFromDirectory(walletFilesDirPath)

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

}