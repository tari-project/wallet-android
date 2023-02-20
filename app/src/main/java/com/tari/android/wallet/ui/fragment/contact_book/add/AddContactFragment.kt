package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentContactsAddBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.ContactListAdapter

class AddContactFragment : CommonFragment<FragmentContactsAddBinding, AddContactViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsAddBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AddContactViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
    }

    private fun setupUI() = with(ui) {
    }
}

