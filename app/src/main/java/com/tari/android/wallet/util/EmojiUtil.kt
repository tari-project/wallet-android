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

import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UProperty
import com.ibm.icu.text.BreakIterator

/**
 * String code points as list.
 */
private fun String.codePointsAsList(): List<Int> {
    val codePoints = mutableListOf<Int>()
    this.codePoints().forEachOrdered {
        for (i in 0 until UCharacter.charCount(it)) {
            codePoints.add(it)
        }
    }
    return codePoints
}

/**
 * Checks whether the unicode code point is some short of an emoji character.
 */
private fun codePointHasEmojiProperty(codePoint: Int): Boolean {
    return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_COMPONENT)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER_BASE)
            || UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_PRESENTATION)
}

/**
 * Number of emojis in a string.
 */
internal fun String.numberOfEmojis(): Int {
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        var isEmoji = true
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            // check if current code point is a part of an emoji
            isEmoji = isEmoji && codePointHasEmojiProperty(codePoint)
        }
        if (isEmoji) {
            emojiCount++
        }
        previous = it.current()
    }
    // no emojis found
    return emojiCount
}

/**
 * @return false if there is at least 1 non-emoji character in the string.
 */
internal fun String.containsNonEmoji(): Boolean {
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            if (!codePointHasEmojiProperty(codePoint)) {
                return true
            }
        }
        previous = it.current()
    }
    // no emojis found
    return false
}

/**
 * Checks whether a given number of first characters of the string are emojis.
 */
internal fun String.firstNCharactersAreEmojis(n: Int): Boolean {
    // prepare a map of codepoints for each character
    val codePoints = this.codePointsAsList()
    // iterate through the string
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(this)
    var emojiCount = 0
    var previous = 0
    while (it.next() != BreakIterator.DONE) {
        for (i in previous until it.current()) {
            val codePoint = codePoints[i]
            // check if current code point is a part of an emoji
            if (!codePointHasEmojiProperty(codePoint)) {
                return false
            }
        }
        if (++emojiCount >= n) {
            return true
        }
        previous = it.current()
    }
    // didn't reach the number of emojis (n)
    return false
}

/**
 * Emoji utility functions.
 *
 * @author The Tari Development Team
 */
internal class EmojiUtil {

    companion object {

        /**
         * @return a shortened 12-character emoji id, consisting
         * of the first, middle and last 4 characters concatenated.
         * Null if the input is not an emoji id.
         */
        fun getShortenedEmojiId(emojiId: String): String? {
            if (emojiId.numberOfEmojis() < Constants.Wallet.emojiIdLength
                || emojiId.containsNonEmoji()
            ) {
                return null
            }
            val emojiIds = ArrayList<String>()
            var currentIndex = 0
            // prep the iterator
            val it: BreakIterator = BreakIterator.getCharacterInstance()
            it.setText(emojiId)
            var previous = 0
            while (it.next() != BreakIterator.DONE) {
                val builder = StringBuilder()
                for (i in previous until it.current()) {
                    builder.append(emojiId[i])
                    currentIndex++
                }
                emojiIds.add(builder.toString())
                previous = it.current()
            }
            val startChunk = emojiIds.take(Constants.Wallet.emojiFormatterChunkSize).joinToString("")

            val middleChunkStartIndex =
                Constants.Wallet.emojiIdLength / 2 - Constants.Wallet.emojiFormatterChunkSize / 2
            val middleChunk = emojiIds.subList(
                middleChunkStartIndex,
                middleChunkStartIndex + Constants.Wallet.emojiFormatterChunkSize
            ).joinToString("")

            val endChunk = emojiIds.takeLast(Constants.Wallet.emojiFormatterChunkSize).joinToString("")

            return startChunk + middleChunk + endChunk
        }

        /**
         * Masking-related: get the indices of current chunk separators.
         *
         * @param string possibly chunked string
         * @param emojiIdChunkSeparator chunk separator sequence
         */
        fun getExistingChunkSeparatorIndices(
            string: String,
            emojiIdChunkSeparator: String
        ): ArrayList<Int> {
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
                    && noOfElements % Constants.Wallet.emojiFormatterChunkSize == 0
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

        fun getChunkedEmojiId(emojiId: String, separator: String): String {
            // make chunks
            val separatorIndices = getNewChunkSeparatorIndices(emojiId)
            val builder = java.lang.StringBuilder(emojiId)
            for ((i, index) in separatorIndices.iterator().withIndex()) {
                builder.insert((index + i), separator)
            }
            return builder.toString()
        }

    }

}
