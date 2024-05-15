package com.tari.android.wallet.ui.fragment.chat_list.data

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsRepository @Inject constructor(private val chatsPrefRepository: ChatsPrefRepository) : CommonViewModel() {

    val list = MutableLiveData<MutableList<ChatItemDto>>()

    init {
        component.inject(this)

        chatsPrefRepository.updateNotifier.subscribe {
            updateList()
        }.addTo(compositeDisposable)
    }

    private fun updateList() {
        val list = chatsPrefRepository.getSavedChats().toMutableList()

        if (DebugConfig.mockChatMessages && list.isEmpty()) {
            val mockedList = mutableListOf(
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "first",
                            "2 days ago",
                            false,
                            false
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                ),
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "second",
                            "2 days ago",
                            true,
                            false
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                ),
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "third",
                            "2 days ago",
                            false,
                            true
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                ),
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "fourth",
                            "2 days ago",
                            true,
                            true
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                ),
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "fifth",
                            "2 days ago",
                            false,
                            false
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                ),
                ChatItemDto(
                    UUID.randomUUID().toString(),
                    listOf(
                        ChatMessageItemDto(
                            UUID.randomUUID().toString(),
                            "sixth",
                            "2 days ago",
                            false,
                            false
                        )
                    ),
                    MockDataStub.WALLET_ADDRESS
                )
            )

            list.addAll(mockedList)
        }

        this.list.postValue(list)
    }

    fun getByUuid(uuid: String): ChatItemDto? = list.value?.find { it.uuid == uuid }

    fun getByWalletAddress(walletAddress: TariWalletAddress): ChatItemDto? = list.value?.find { it.walletAddress == walletAddress }

    fun addChat(chat: ChatItemDto) = chatsPrefRepository.addChat(chat)

    fun addMessage(walletAddress: TariWalletAddress, message: ChatMessageItemDto) =
        chatsPrefRepository.saveMessage(getByWalletAddress(walletAddress), message)
}