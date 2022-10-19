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
package com.tari.android.wallet.ui.presentation

import com.tari.android.wallet.model.TxNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TxNoteTest {

    @Test(expected = IllegalStateException::class)
    fun `compose, assert that IllegalStateException was thrown if both arguments are null pointers`() {
        TxNote(null, null)
    }

    @Test
    fun `compose, assert that only message was included if gif is null`() {
        val givenMessage = "alala"
        assertEquals(
            givenMessage, TxNote(
                givenMessage,
                null
            ).compose()
        )
    }

    @Test
    fun `compose, assert that only url with a whitespace was included if message is null`() {
        val givenUrl = "https://giphy.com/embed/l2Sq9qGTQnL5NyI6Y"
        assertEquals(
            " $givenUrl", TxNote(
                null,
                givenUrl
            ).compose()
        )
    }

    @Test
    fun `compose, assert that message with whitespace and url were included if both arguments point to valid values`() {
        val givenMessage = "bubun"
        val givenUrl = "https://giphy.com/embed/l2Sq9qGTQnL5NyI6Y"
        assertEquals(
            "$givenMessage $givenUrl", TxNote(
                givenMessage,
                givenUrl
            ).compose()
        )
    }

    @Test
    fun `fromNote, assert that empty note and no gif will be returnedif note is empty`() {
        val note = TxNote.fromNote("")
        assertEquals("", note.message)
        assertNull(note.gifUrl)
    }

    @Test
    fun `fromNote, assert that only message was included if gif is null`() {
        val givenMessage = "alala"
        assertEquals(TxNote(givenMessage, null), TxNote.fromNote(givenMessage))
    }

    @Test
    fun `fromNote, assert that only url with a whitespace was included if message is null`() {
        val givenUrl = "https://giphy.com/embed/l2Sq9qGTQnL5NyI6Y"
        assertEquals(TxNote(null, givenUrl), TxNote.fromNote(" $givenUrl"))
    }

    @Test
    fun `fromNote, assert that message with whitespace and url were included if both arguments point to valid values`() {
        val givenMessage = "bubun"
        val givenUrl = "https://giphy.com/embed/l2Sq9qGTQnL5NyI6Y"
        assertEquals(
            TxNote(givenMessage, givenUrl),
            TxNote.fromNote("$givenMessage $givenUrl")
        )
    }

    @Test
    fun `gifId, assert that null was returned if gif is null`() {
        assertNull(TxNote("asd", null).gifId)
    }

    @Test
    fun `gifId, assert that last url part was returned if gif is not null`() {
        val id = "l2Sq9qGTQnL5NyI6Y"
        val givenUrl = "https://giphy.com/embed/$id"
        assertEquals(id, TxNote(null, givenUrl).gifId)
    }

}
