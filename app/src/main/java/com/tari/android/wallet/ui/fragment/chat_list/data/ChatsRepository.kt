package com.tari.android.wallet.ui.fragment.chat_list.data

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.TariBuild
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsRepository @Inject constructor() : CommonViewModel() {

    val list = MutableLiveData<MutableList<ChatItemDto>>()

    init {
        component.inject(this)

        if (TariBuild.MOCKED) {
            val list = mutableListOf(
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
                    TariBuild.mocked_wallet_address
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
                    TariBuild.mocked_wallet_address
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
                    TariBuild.mocked_wallet_address
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
                    TariBuild.mocked_wallet_address
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
                    TariBuild.mocked_wallet_address
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
                    TariBuild.mocked_wallet_address
                )
            )

            this.list.postValue(list)
        }
    }
}