package com.tari.android.wallet.ui.screen.chat.chatList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentChatListBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.screen.chat.chatList.adapter.ChatItemViewHolder
import com.tari.android.wallet.ui.screen.chat.chatList.adapter.ChatItemViewHolderItem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.setVisible

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
                viewModel.onChatClicked(it)
            }
        })

        toolbar.setRightArgs(TariToolbarActionArg(R.drawable.vector_chat_add) {
            viewModel.onAddChatClicked()
        })
    }

    private fun subscribeUI() {
        collectFlow(viewModel.uiState) { uiState ->
            adapter.submitList(uiState.chatList)
            ui.list.setVisible(!uiState.showEmpty)
            ui.emptyState.setVisible(uiState.showEmpty)
        }
    }
}