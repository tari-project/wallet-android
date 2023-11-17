package com.tari.android.wallet.ui.fragment.chat_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentChatListBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.chat_list.adapter.ChatItemViewHolder
import com.tari.android.wallet.ui.fragment.chat_list.adapter.ChatItemViewHolderItem
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation

class ChatListFragment : CommonFragment<FragmentChatListBinding, ChatListViewModel>() {

    val adapter: CommonAdapter<CommonViewHolderItem> by lazy { AdapterFactory.generate(ChatItemViewHolder.getBuilder()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChatListBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChatListViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()

        subscribeUI()
    }

    private fun initUI() = with(ui) {
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        adapter.setClickListener(CommonAdapter.ItemClickListener {
            if (it is ChatItemViewHolderItem) {
                viewModel.navigation.postValue(Navigation.ChatNavigation.ToChat(it.walletAddress, false))
            }
        })

        val right = TariToolbarActionArg(R.drawable.vector_chat_add) {
            viewModel.navigation.postValue(Navigation.ChatNavigation.ToAddChat)
        }
        toolbar.setRightArgs(right)
    }

    private fun subscribeUI() = with(viewModel) {
        observe(list) {
            adapter.submitList(it)
            ui.list.setVisible(it.isNotEmpty())
            ui.emptyState.setVisible(it.isEmpty())
        }
    }
}