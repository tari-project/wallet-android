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
package com.tari.android.wallet.util

import android.icu.text.BreakIterator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.tari.android.wallet.ffi.FFIEmojiSet
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.extension.applyColorStyle
import com.tari.android.wallet.util.extension.applyLetterSpacingStyle
import com.tari.android.wallet.util.extension.applyRelativeTextSizeStyle

/**
 * Number of emojis from the Tari emoji set in a string.
 */
fun EmojiId.numberOfEmojis(emojiSet: Set<EmojiId> = EmojiUtil.FFI_EMOJI_SET): Int {
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    val codepointBuilder = StringBuilder()
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            codepointBuilder.append(this[i])
        }
        val codepoint = codepointBuilder.toString()
        if (emojiSet.contains(codepoint)) {
            emojiCount++
        }
        previous = it.current()
        codepointBuilder.clear()
    }
    return emojiCount
}

/**
 * @return true if there is at least 1 character that is not included in the Tari emoji set.
 */
fun EmojiId.containsNonEmoji(emojiSet: Set<EmojiId> = EmojiUtil.FFI_EMOJI_SET): Boolean {
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var previous = 0
    val codepointBuilder = StringBuilder()
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            codepointBuilder.append(this[i])
        }
        val codepoint = codepointBuilder.toString()
        if (!emojiSet.contains(codepoint)) {
            return true
        }
        previous = it.current()
        codepointBuilder.clear()
    }
    // no emojis found
    return false
}

/**
 * @return emojis in the string that are from the Tari emoji set
 */
fun EmojiId.extractEmojis(emojiSet: Set<String> = EmojiUtil.FFI_EMOJI_SET): List<EmojiId> {
    // iterate through the codepoints
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var previous = 0
    val codepointBuilder = StringBuilder()
    val emojis = mutableListOf<String>()
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            codepointBuilder.append(this[i])
        }
        val codepoint = codepointBuilder.toString()
        if (emojiSet.contains(codepoint)) {
            emojis.add(codepoint)
        }
        codepointBuilder.clear()
        previous = it.current()
    }
    return emojis
}

/**
 * Checks whether a given number of first characters of the string are emojis from the Tari
 * emoji set.
 */
fun String.firstNCharactersAreEmojis(n: Int, emojiSet: Set<String> = EmojiUtil.FFI_EMOJI_SET): Boolean {
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    val codepointBuilder = StringBuilder()
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            codepointBuilder.append(this[i])
        }
        val codepoint = codepointBuilder.toString()
        if (emojiSet.contains(codepoint)) {
            if (++emojiCount >= n) {
                return true
            }
        } else {
            return false
        }
        previous = it.current()
        codepointBuilder.clear()
    }
    // didn't reach the number of emojis (n)
    return false
}

fun Int.tariEmoji(): EmojiId {
    return EmojiUtil.FFI_EMOJI_SET.elementAt(this)
}

/**
 * Emoji utility functions.
 *
 * @author The Tari Development Team
 */
class EmojiUtil {

    companion object {

        const val SMALL_EMOJI_ID_SIZE = 6

        val FFI_EMOJI_SET: Set<EmojiId> by lazy {
            val emojis = mutableSetOf<EmojiId>()
            val emojiSetFFI = FFIEmojiSet()
            for (i in 0 until emojiSetFFI.getLength()) {
                val emojiFFI = emojiSetFFI.getAt(i)
                val emojiBytes = emojiFFI.byteArray()
                val emoji = String(emojiBytes)
                emojis.add(emoji)
                emojiFFI.destroy()
            }
            emojiSetFFI.destroy()
            emojis
        }

        /**
         * Masking-related: get the indices of current chunk separators.
         *
         * @param string possibly chunked string
         * @param emojiIdChunkSeparator chunk separator sequence
         */
        fun getExistingChunkSeparatorIndices(string: String, emojiIdChunkSeparator: String): ArrayList<Int> {
            val existingIndices = ArrayList<Int>()
            var currentIndex = 0
            // prep the iterator
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            while (it.next() != BreakIterator.DONE) {
                val builder = StringBuilder()
                val itemIndex = currentIndex
                for (i in previous until it.current()) {
                    builder.append(string[i])
                    currentIndex++
                }
                val item = builder.toString()
                if (item == emojiIdChunkSeparator) {
                    existingIndices.add(itemIndex)
                }
                previous = it.current()
            }
            return existingIndices
        }

        /**
         * Masking-related: calculate the indices of separators for a string.
         *
         * @param string non-chunked string
         */
        fun getNewChunkSeparatorIndices(string: String): ArrayList<Int> {
            val newIndices = ArrayList<Int>()
            if (string.getGraphemeLength() < SMALL_EMOJI_ID_SIZE) return newIndices
            var currentIndex = 0
            // prep the iterator
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            var noOfElements = 0
            while (it.next() != BreakIterator.DONE) {
                val builder = StringBuilder()
                for (i in previous until it.current()) {
                    builder.append(string[i])
                    currentIndex++
                }
                noOfElements++
                if (currentIndex < string.length
                    && noOfElements % Constants.Wallet.EMOJI_FORMATTER_CHUNK_SIZE == 0
                ) {
                    newIndices.add(currentIndex)
                }
                previous = it.current()
            }
            return newIndices
        }

        fun getStartIndexOfItemEndingAtIndex(string: String, endIndex: Int): Int {
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(string)
            var previous = 0
            while (it.next() != BreakIterator.DONE) {
                if (it.current() == endIndex) {
                    return previous
                }
                previous = it.current()
            }
            return -1
        }

        private fun getChunkedEmojiId(emojiId: EmojiId, separator: String): String {
            // make chunks
            val separatorIndices = getNewChunkSeparatorIndices(emojiId)
            val builder = java.lang.StringBuilder(emojiId)
            for ((i, index) in separatorIndices.iterator().withIndex()) {
                builder.insert((index + i * separator.length), separator)
            }
            return builder.toString()
        }

        fun getChunkSeparatorSpannable(separator: String, color: Int): SpannableString {
            val spannable = SpannableString(separator)
            spannable.setSpan(ForegroundColorSpan(color), 0, separator.length, Spanned.SPAN_INTERMEDIATE)
            spannable.applyRelativeTextSizeStyle(separator, Constants.UI.EMOJI_ID_CHUNK_SEPARATOR_RELATIVE_SCALE)
            spannable.applyLetterSpacingStyle(separator, Constants.UI.EMOJI_ID_CHUNK_SEPARATOR_LETTER_SPACING)
            return spannable
        }

        fun getFullEmojiIdSpannable(emojiId: EmojiId, separator: String, darkColor: Int, lightColor: Int): SpannableString {
            val spannable = getChunkedEmojiId(emojiId, separator).applyColorStyle(
                defaultColor = darkColor,
                search = listOf(separator),
                styleColor = lightColor,
                applyToOnlyFirstOccurrence = false,
            )
            spannable.applyLetterSpacingStyle(separator, Constants.UI.EMOJI_ID_CHUNK_SEPARATOR_LETTER_SPACING)
            spannable.applyRelativeTextSizeStyle(separator, Constants.UI.EMOJI_ID_CHUNK_SEPARATOR_RELATIVE_SCALE, applyToOnlyFirstOccurrence = false)
            return spannable
        }

        fun EmojiId.getGraphemeLength(): Int {
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(this)
            var count = 0
            while (it.next() != BreakIterator.DONE) {
                count++
            }
            return count
        }
    }
}

/* new address format */

/**
 * Returns the prefix of the address in the format "| prefix | address1 ••• address2". E.g. "| 🐢💤| 🐉🔋😎 ••• 🍭🎤💍".
 */
fun TariWalletAddress.addressPrefixEmojis(): EmojiId {
    return this.networkEmoji + this.featuresEmoji
}

/**
 * Returns the first 3 emojis of the address in the format "| prefix | address1 ••• address2". E.g. "| 🐢💤| 🐉🔋😎 ••• 🍭🎤💍".
 */
fun TariWalletAddress.addressFirstEmojis(): EmojiId {
    return this.coreKeyEmojis.extractEmojis().take(3).joinToString("")
}

/**
 * Returns the last 3 emojis of the address in the format "| prefix | address1 ••• address2". E.g. "| 🐢💤| 🐉🔋😎 ••• 🍭🎤💍".
 */
fun TariWalletAddress.addressLastEmojis(): EmojiId {
    return this.coreKeyEmojis.extractEmojis().takeLast(3).joinToString("")
}

/**
 * Returns a string with the address in the format "prefix|address1•••address2". E.g. "🐢💤|🐉🔋😎•••🍭🎤💍".
 */
fun TariWalletAddress.shortString(): String = this.addressPrefixEmojis() + "|" + this.addressFirstEmojis() + "..." + this.addressLastEmojis()

/**
 * Returns a string with the Base58 address in the format of "AAAAAA...BBBBBB"
 */
fun TariWalletAddress.base58Ellipsized(charCount: Int = 6): String {
    return fullBase58.substring(0, charCount) + "..." + fullBase58.substring(fullBase58.length - charCount)
}
