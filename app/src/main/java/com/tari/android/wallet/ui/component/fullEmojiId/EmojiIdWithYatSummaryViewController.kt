package com.tari.android.wallet.ui.component.fullEmojiId

import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewEmojiIdWithYatSummaryBinding
import com.tari.android.wallet.ui.extension.setVisible

/**
 * Display a summary of the emoji id - with pipes.
 *
 * @author The Tari Development Team
 */
// TODO check we don't use it anymore and delete
class EmojiIdWithYatSummaryViewController(private val ui: ViewEmojiIdWithYatSummaryBinding) {

    var isYatForegrounded = false
        set(value) {
            field = value
            display()
        }

    var yat: String = ""
        set(value) {
            field = value
            isYatForegrounded = value.isNotEmpty()
            display()
        }
    
    var emojiId = ""
        set(value) {
            field = value
            display()
        }


    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)

    init {
        ui.yatButton.setOnClickListener {
            isYatForegrounded = !isYatForegrounded
        }
        ui.emojiIdSummaryView
    }

    fun display() {
        ui.yatButton.setVisible(yat.isNotEmpty())

        val icon = if (isYatForegrounded) R.drawable.vector_tari_yat_open else R.drawable.vector_tari_yat_close
        val drawable = ContextCompat.getDrawable(ui.root.context, icon)
        ui.yatButton.setImageDrawable(drawable)

        emojiIdSummaryController.display(if (yat.isNotEmpty() && isYatForegrounded) yat else emojiId)
    }
}
