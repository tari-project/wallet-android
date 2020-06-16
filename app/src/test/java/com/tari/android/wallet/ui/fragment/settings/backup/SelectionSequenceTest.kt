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
package com.tari.android.wallet.ui.fragment.settings.backup

import com.tari.android.wallet.ui.fragment.settings.backup.VerifySeedPhraseFragment.Phrase
import com.tari.android.wallet.ui.fragment.settings.backup.VerifySeedPhraseFragment.SelectionSequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionSequenceTest {

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException is thrown if selection index is equal to the phrase length`() {
        val givenWord = "gword"
        val originalPhrase = Phrase(listOf(givenWord))
        SelectionSequence(originalPhrase, originalPhrase).apply { add(originalPhrase.length) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException is thrown if selection index is negative`() {
        val givenWord = "gword"
        val originalPhrase = Phrase(listOf(givenWord))
        SelectionSequence(originalPhrase, originalPhrase).apply { add(-1) }
    }

    @Test
    fun `assert that one-word sequence having a word from one-word phrase matches the original phrase`() {
        val givenWord = "gword"
        val original = Phrase(listOf(givenWord))
        val seq = SelectionSequence(original, original).apply { add(0) }
        assertTrue(seq.matchesOriginalPhrase())
    }

    @Test
    fun `assert that current selection returns an expected set of values`() {
        val givenWords = listOf("w1", "w2", "w3")
        val original = Phrase(givenWords)
        val seq = SelectionSequence(original, Phrase(givenWords.reversed())).apply {
            add(0)
            add(1)
        }
        assertEquals(listOf("w3", "w2"), seq.currentSelection.map(Pair<Int, String>::second))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException was thrown if given phrases do not have the same length`() {
        val original = Phrase(listOf("1", "2"))
        SelectionSequence(original, Phrase(listOf("1")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException was thrown if size is already matches phrase length`() {
        val sequence = Phrase(listOf("1", "2", "3")).startSelection().second
            .apply { (0..2).forEach(this::add) }
        sequence.add(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException was thrown if a word was already selected`() {
        Phrase(listOf("1", "2", "3")).startSelection().second.apply {
            add(0)
            add(0)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assert that IllegalArgumentException was thrown if an index that's not added was removed`() {
        Phrase(listOf("1", "2", "3")).startSelection().second.apply {
            remove(0)
        }
    }

    @Test
    fun `assert that an added index can be removed OK`() {
        Phrase(listOf("1", "2", "3")).startSelection().second.apply {
            add(0)
            remove(0)
        }
    }

    @Test
    fun `assert that remove operation has complementary semantics in relation to add`() {
        val phrase = Phrase(listOf("1", "2", "3"))
        SelectionSequence(phrase, phrase).apply {
            add(0)
            remove(0)
            (0..2).forEach(::add)
        }.matchesOriginalPhrase()
            .apply(::assertTrue)
    }

}
