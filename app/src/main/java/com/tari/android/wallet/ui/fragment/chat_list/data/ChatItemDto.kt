package com.tari.android.wallet.ui.fragment.chat_list.data

import com.tari.android.wallet.model.TariWalletAddress
import java.io.Serializable

class ChatItemDto(val uuid: String, val messages: List<ChatMessageItemDto>, val walletAddress: TariWalletAddress) : Serializable