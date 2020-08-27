package com.tari.android.wallet.model.yat

import com.tari.android.wallet.util.containsNonEmoji

data class EmojiId(val raw: String) {

    fun noVariationSelectors() = EmojiId(raw.filter { it != '\uFE0F' })

    companion object {
        fun of(input: String, set: EmojiSet): EmojiId? =
            if (input.isNotEmpty() && !input.containsNonEmoji(set.set!!)) EmojiId(input) else null
    }

}
