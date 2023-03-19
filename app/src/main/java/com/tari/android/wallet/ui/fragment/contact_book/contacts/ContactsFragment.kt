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
package com.tari.android.wallet.ui.fragment.contact_book.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentContactsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.PermissionExtensions.runWithPermission
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.ContactListAdapter

open class ContactsFragment : CommonFragment<FragmentContactsBinding, ContactsViewModel>() {

    private var recyclerViewAdapter: ContactListAdapter = ContactListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactsViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.serviceConnection.reconnectToService()

        setupUI()
        observeUI()

        grantPermission(false)
    }

    override fun onStart() {
        super.onStart()
        grantPermission(false)
    }

    fun search(text: String) = viewModel.search(text)

    private fun observeUI() = with(viewModel) {
        observe(list) { recyclerViewAdapter.update(it) }

        observe(grantPermission) { grantPermission(true) }

        observeOnLoad(listUpdateTrigger)
        observeOnLoad(debouncedList)
    }

    private fun setupUI() = with(ui) {
        setupRecyclerView()
    }

    private fun grantPermission(withSettings: Boolean) {
        runWithPermission(android.Manifest.permission.READ_CONTACTS, withSettings) {
            viewModel.contactsRepository.phoneBookRepositoryBridge.synchronize()
            viewModel.contactPermission.postValue(true)
        }
    }

    private fun setupRecyclerView() {
        ui.contactsListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.processItemClick(it) })
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
    }
}


