package com.tari.android.wallet.ui.fragment.contact_book.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentContactsLinkBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.link.adapter.LinkContactAdapter
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_CONTACT

class ContactLinkFragment : CommonFragment<FragmentContactsLinkBinding, ContactLinkViewModel>() {

    private val adapter: LinkContactAdapter by lazy { LinkContactAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsLinkBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactLinkViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.initArgs(requireArguments().parcelable<ContactDto>(PARAMETER_CONTACT)!!)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) { adapter.update(it) }

        observe(grantPermission) { viewModel.grantPermission() }
    }

    private fun initUI() = with(ui) {
        listUi.adapter = adapter
        listUi.layoutManager = LinearLayoutManager(context)

        adapter.setClickListener(CommonAdapter.ItemClickListener { item -> viewModel.onContactClick(item) })
    }

    companion object {
        fun createFragment(args: ContactDto): ContactLinkFragment = ContactLinkFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, args) }
        }
    }
}

