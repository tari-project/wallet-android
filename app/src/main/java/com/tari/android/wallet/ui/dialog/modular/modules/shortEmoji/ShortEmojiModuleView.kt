package com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleShortEmojiBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

@SuppressLint("ViewConstructor")
class ShortEmojiModuleView(context: Context, shortEmojiModule: ShortEmojiIdModule) :
    CommonView<CommonViewModel, DialogModuleShortEmojiBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleShortEmojiBinding =
        DialogModuleShortEmojiBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.emojiIdViewContainer.textViewEmojiPrefix.text = shortEmojiModule.tariWalletAddress.addressPrefixEmojis()
        ui.emojiIdViewContainer.textViewEmojiFirstPart.text = shortEmojiModule.tariWalletAddress.addressFirstEmojis()
        ui.emojiIdViewContainer.textViewEmojiLastPart.text = shortEmojiModule.tariWalletAddress.addressLastEmojis()
    }
}