/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.component

import android.icu.text.BreakIterator
import android.view.View
import com.tari.android.wallet.databinding.EmojiIdSummaryBinding
import com.tari.android.wallet.ui.extension.setVisible

/**
 * Display a summary of the emoji id - with pipes.
 *
 * @author The Tari Development Team
 */
internal class EmojiIdSummaryViewController(private val ui: EmojiIdSummaryBinding) {

    constructor(view: View) : this(EmojiIdSummaryBinding.bind(view))

    fun display(emojiId: String, showEmojisFromEachEnd: Int = 3) {

        val emojis = ArrayList<String>()
        val it: BreakIterator = BreakIterator.getCharacterInstance()
        it.setText(emojiId)
        var previous = 0
        while (it.next() != BreakIterator.DONE) {
            val builder = StringBuilder()
            for (i in previous until it.current()) {
                builder.append(emojiId[i])
            }
            emojis.add(builder.toString())
            previous = it.current()
        }

        if (showEmojisFromEachEnd > 3) {
            throw IllegalArgumentException("Cannot show more than 3 emojis from each end.")
        } else if (showEmojisFromEachEnd < 2) {
            throw IllegalArgumentException("Cannot show less than 2 emojis from each end.")
        }

        val textViews = arrayListOf(
            ui.emojiIdSummaryEmoji1TextView,
            ui.emojiIdSummaryEmoji2TextView,
            ui.emojiIdSummaryEmoji3TextView,
            ui.emojiIdSummaryEmoji4TextView,
            ui.emojiIdSummaryEmoji5TextView,
            ui.emojiIdSummaryEmoji6TextView,
        )
        textViews.forEach { it.setVisible(false) }

        if (emojis.size <= showEmojisFromEachEnd * 2) {
            ui.emojiIdSummaryTxtEmojiSeparator.setVisible(false)

            for ((index, emoji) in emojis.withIndex()) {
                textViews[index].text = emoji
                textViews[index].setVisible(true)
            }
        } else {
            ui.emojiIdSummaryTxtEmojiSeparator.setVisible(true)
            for (i in 0 until showEmojisFromEachEnd) {
                textViews[i].text = emojis[i]
                textViews[i].setVisible(true)
            }

            for (i in 0 until showEmojisFromEachEnd) {
                textViews[showEmojisFromEachEnd + i].text = emojis[emojis.size - i - 1]
                textViews[showEmojisFromEachEnd + i].setVisible(true)
            }
        }
    }
}
