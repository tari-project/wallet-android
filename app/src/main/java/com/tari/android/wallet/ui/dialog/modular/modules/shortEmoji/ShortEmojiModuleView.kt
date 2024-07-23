package com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleShortEmojiBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController

@SuppressLint("ViewConstructor")
class ShortEmojiModuleView(context: Context, buttonModule: ShortEmojiIdModule) : CommonView<CommonViewModel, DialogModuleShortEmojiBinding>(context) {

    private val emojiController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleShortEmojiBinding =
        DialogModuleShortEmojiBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        emojiController.display(buttonModule.tariWalletAddress.fullEmojiId)
    }
}