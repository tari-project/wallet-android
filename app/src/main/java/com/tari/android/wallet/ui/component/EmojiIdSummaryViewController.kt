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

import android.view.View
import android.icu.text.BreakIterator
import com.tari.android.wallet.databinding.EmojiIdSummaryBinding

/**
 * Display a summary of the emoji id - with pipes.
 *
 * @author The Tari Development Team
 */
internal class EmojiIdSummaryViewController(private val ui: EmojiIdSummaryBinding) {

    constructor(view: View) : this(EmojiIdSummaryBinding.bind(view))

    fun display(emojiId: String) {
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

        ui.emojiIdSummaryEmoji1TextView.text = emojis[0]
        ui.emojiIdSummaryEmoji2TextView.text = emojis[1]
        ui.emojiIdSummaryEmoji3TextView.text = emojis[2]
        ui.emojiIdSummaryEmoji4TextView.text = emojis.takeLast(3)[0]
        ui.emojiIdSummaryEmoji5TextView.text = emojis.takeLast(2)[0]
        ui.emojiIdSummaryEmoji6TextView.text = emojis.takeLast(1)[0]
    }


}
