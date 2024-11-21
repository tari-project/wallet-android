package com.tari.android.wallet.ui.fragment.contactBook.link

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentContactsLinkBinding
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.takeIfIs
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactItemViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.link.adapter.LinkContactAdapter

class ContactLinkFragment : CommonFragment<FragmentContactsLinkBinding, ContactLinkViewModel>() {

    private val adapter: LinkContactAdapter by lazy { LinkContactAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsLinkBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactLinkViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        collectFlow(uiState) { adapter.update(it.viewItemList) }

        collectFlow(effect) { effect ->
            when (effect) {
                is ContactLinkModel.Effect.GrantPermission -> viewModel.grantPermission()
            }
        }
    }

    private fun initUI() = with(ui) {
        listUi.adapter = adapter
        listUi.layoutManager = LinearLayoutManager(context)

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            item.takeIfIs<ContactItemViewHolderItem>()?.let { viewModel.onContactClick(it) }
        })
    }

    companion object {
        fun createFragment(args: ContactDto): ContactLinkFragment = ContactLinkFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, args) }
        }
    }
}
