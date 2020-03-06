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
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.WalletUtil
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Debug: show list of log files.
 *
 * @author The Tari Development Team
 */
class DebugLogFilePickerFragment : BaseFragment() {

    @BindView(R.id.log_file_vw_files_listview)
    lateinit var logFileListView: ListView

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String

    private var listener: Listener? = null
    override val contentViewId: Int
        get() = R.layout.fragment_log_file_list

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
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

        val fileList = WalletUtil.getLogFilesFromDirectory(walletFilesDirPath)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(
                context!!,
                android.R.layout.simple_list_item_1,
                getFileNames(fileList)
            )

        logFileListView.adapter = adapter

        logFileListView.setOnItemClickListener { _, _, position, _ ->
            listener?.onLogFileSelected(position)
        }
    }

    @OnClick(R.id.log_file_btn_back)
    fun onBackButtonPressed(view: View) {
        UiUtil.temporarilyDisableClick(view)
        activity?.supportFragmentManager?.popBackStack()

    }

    private fun getFileNames(files: List<File>): List<String> {
        val filesName = ArrayList<String>()
        files.forEach { file -> filesName.add(file.name) }
        return filesName
    }

    interface Listener {
        fun onLogFileSelected(position: Int)
    }
}