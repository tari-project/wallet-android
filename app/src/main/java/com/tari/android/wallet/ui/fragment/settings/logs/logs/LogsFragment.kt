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
package com.tari.android.wallet.ui.fragment.settings.logs.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentLogsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.LogListAdapter
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.LogViewHolderItem
import java.io.File

class LogsFragment : CommonFragment<FragmentLogsBinding, LogsViewModel>() {

    private lateinit var recyclerViewAdapter: LogListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentLogsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: LogsViewModel by viewModels()
        bindViewModel(viewModel)

        arguments?.getSerializable(DebugActivity.log_file, File::class.java)?.let {
            this.ui.title.text = it.name
            viewModel.initWithFile(it)
        }

        setupUI()
        observeUI()
    }

    private fun setupUI() = with(ui) {
        filterButton.setVisible(false)
        backCtaView.setOnClickListener { requireActivity().onBackPressed() }
        filterButton.setOnClickListener { viewModel.showFilters() }
        recyclerViewAdapter = LogListAdapter()
        recyclerViewAdapter.setLongClickListener(CommonAdapter.ItemLongClickListener {
            viewModel.copyToClipboard(it)
            true
        })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recyclerViewAdapter
    }

    private fun observeUI() = with(viewModel) {
        observe(filteredLogs) { updateData(it) }
    }

    private fun updateData(list: MutableList<LogViewHolderItem>) {
        ui.loadingState.setVisible(false)
        ui.filterButton.setVisible(true)
        recyclerViewAdapter.update(list)
    }

    companion object {
        fun getInstance(file: File): LogsFragment = LogsFragment().apply {
            arguments = Bundle().apply {
                putSerializable(DebugActivity.log_file, file)
            }
        }
    }
}