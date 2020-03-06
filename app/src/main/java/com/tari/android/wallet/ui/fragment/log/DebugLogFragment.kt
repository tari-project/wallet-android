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
package com.tari.android.wallet.ui.fragment.log

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.EditText
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
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
class DebugLogFragment : BaseFragment() {
    override val contentViewId: Int
        get() = R.layout.fragment_debug_log

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String
    @Inject
    @Named(WalletModule.FieldName.walletLogFilePath)
    internal lateinit var logFilePath: String

    @BindView(R.id.debug_log_multiline_edit)
    lateinit var multilineEdit: EditText

    private var listener: DebugListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is DebugListener) {
            listener = context
        } else {
            throw AssertionError("Activity must implement listener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    @OnClick(R.id.debug_log_img_btn_more_file_list)
    fun onMoreLogsFileClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listener?.onFilePickerClick()
    }

    @OnClick(R.id.debug_log_btn_back)
    fun onBackButtonPressed(view: View) {
        UiUtil.temporarilyDisableClick(view)
        activity?.finish()
        activity?.overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun setupUi() {
        val files = WalletUtil.getLogFilesFromDirectory(walletFilesDirPath)
        if (!files.isNullOrEmpty()) {
            val logFile = files[0]
            fetchLogFromFile(logFile)
        }

    }

    private fun fetchLogFromFile(logFile: File) {
        multilineEdit.text.clear()
        multilineEdit.setHorizontallyScrolling(true)
        multilineEdit.movementMethod = ScrollingMovementMethod()

        val lineList = mutableListOf<String>()

        if (logFile.exists()) {
            val inputStream: InputStream = logFile.inputStream()
            inputStream.bufferedReader()
                .useLines { lines -> lines.forEach { lineList.add(it) } }
        } else {
            lineList.add("No log available")
        }
        lineList.forEach { multilineEdit.append(it + "\n") }
    }

    fun refreshLogs(position: Int) {
        val files = WalletUtil.getLogFilesFromDirectory(walletFilesDirPath)
        fetchLogFromFile(files[position])
    }

    interface DebugListener {
        fun onFilePickerClick()
    }
}