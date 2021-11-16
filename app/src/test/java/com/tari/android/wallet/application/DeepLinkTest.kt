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
package com.tari.android.wallet.application

import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {

    private val networkRepository: NetworkRepository = NetworkRepositoryMock()
    private val currentNetwork = Network.WEATHERWAX

    @Test
    fun `from, assert that emoji id for testnet is deserialized correctly`() {
        val givenLink = "tari://${currentNetwork.uriComponent}/eid/$EMOJI_SEQUENCE"
        val result = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(result.network, currentNetwork)
        assertEquals(result.type, DeepLink.Type.EMOJI_ID)
        assertEquals(result.identifier, EMOJI_SEQUENCE)
    }

    @Test
    fun `from, assert that emoji id for testnet is not deserialized if there are 32 emojis`() {
        val givenLink = "tari://$${currentNetwork.uriComponent}/eid/$CUT_EMOJI_SEQUENCE"
        val result = DeepLink.from(networkRepository, givenLink)
        assertNull(result)
    }

    @Test
    fun `from, assert that emoji id for mainnet is deserialized correctly`() {
        val givenLink = "tari://mainnet/eid/$EMOJI_SEQUENCE"
        val result = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(result.network, Network.MAINNET)
        assertEquals(result.type, DeepLink.Type.EMOJI_ID)
        assertEquals(result.identifier, EMOJI_SEQUENCE)
    }

    @Test
    fun `from, assert that emoji id for mainned is not deserialized if there are 32 emojis`() {
        val givenLink = "tari://mainnet/eid/$CUT_EMOJI_SEQUENCE"
        val result = DeepLink.from(networkRepository, givenLink)
        assertNull(result)
    }

    @Test
    fun `from, assert that public key for testnet is deserialized correctly`() {
        val givenLink = "tari://${currentNetwork.uriComponent}/pubkey/$PUBLIC_KEY"
        val result = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(result.network, currentNetwork)
        assertEquals(result.type, DeepLink.Type.PUBLIC_KEY_HEX)
        assertEquals(result.identifier, PUBLIC_KEY)
    }

    @Test
    fun `from, assert that public key for testnet is not deserialized correctly if it consists of 63 symbols`() {
        val givenLink = "tari://${currentNetwork.uriComponent}/pubkey/$PUBLIC_KEY"
        val result = DeepLink.from(networkRepository, givenLink.substring(0, givenLink.length - 1))
        assertNull(result)
    }

    @Test
    fun `from, assert that public key for mainnet is deserialized correctly`() {
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY"
        val result = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(result.network, Network.MAINNET)
        assertEquals(result.type, DeepLink.Type.PUBLIC_KEY_HEX)
        assertEquals(result.identifier, PUBLIC_KEY)
    }

    @Test
    fun `from, assert that public key for mainnet is not deserialized correctly if it consists of 63 symbols`() {
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY"
        val result = DeepLink.from(networkRepository, givenLink.substring(0, givenLink.length - 1))
        assertNull(result)
    }

    @Test
    fun `assert that test emoji id contains exactly 33 code points`() {
        assertEquals(33, EMOJI_SEQUENCE.codePointCount(0, EMOJI_SEQUENCE.length))
    }

    @Test
    fun `assert that test public key contains exactly 64 code points`() {
        assertEquals(64, PUBLIC_KEY.codePointCount(0, PUBLIC_KEY.length))
    }

    @Test
    fun `assert that default note parameter was parsed correctly for mainnet pubkey link`() {
        // %D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82 == привет
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY?note=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82"
        val actual = DeepLink.from(networkRepository, givenLink)!!
        assertEquals("привет", actual.parameters[DeepLink.PARAMETER_NOTE])
        assertEquals(1, actual.parameters.size)
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun `assert that default amount parameter was parsed correctly for mainnet pubkey link`() {
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY?amount=5.553634"
        val actual = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(5.553634, actual.parameters[DeepLink.PARAMETER_AMOUNT]!!.toDouble(), 0.01)
        assertEquals(1, actual.parameters.size)
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun `assert that default amount and note parameters were parsed correctly for mainnet pubkey link`() {
        // %D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82 == привет
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY?amount=5.553634&note=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82"
        val actual = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(5.553634, actual.parameters[DeepLink.PARAMETER_AMOUNT]!!.toDouble(), 0.01)
        assertEquals("привет", actual.parameters[DeepLink.PARAMETER_NOTE])
        assertEquals(2, actual.parameters.size)
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun `assert that default query parameters separator alone is ignored`() {
        // %D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82 == привет
        val givenLink = "tari://mainnet/pubkey/$PUBLIC_KEY?"
        val actual = DeepLink.from(networkRepository, givenLink)!!
        assertEquals(actual.parameters, emptyMap<String, String>())
    }

    companion object {
        // 🤒👅💦👽🐖😥🦓🐱⛑🐼👔🤚🦌😀😱🙄👘😞👁👿😞✊😉💨🐑😊🦓🐗💼🐀🧵😳🦡
        private const val EMOJI_SEQUENCE =
            "\uD83E\uDD12\uD83D\uDC45\uD83D\uDCA6\uD83D\uDC7D\uD83D\uDC16\uD83D\uDE25\uD83E\uDD93\uD83D\uDC31⛑\uD83D\uDC3C\uD83D\uDC54\uD83E\uDD1A\uD83E\uDD8C\uD83D\uDE00\uD83D\uDE31\uD83D\uDE44\uD83D\uDC58\uD83D\uDE1E\uD83D\uDC41\uD83D\uDC7F\uD83D\uDE1E✊\uD83D\uDE09\uD83D\uDCA8\uD83D\uDC11\uD83D\uDE0A\uD83E\uDD93\uD83D\uDC17\uD83D\uDCBC\uD83D\uDC00\uD83E\uDDF5\uD83D\uDE33\uD83E\uDDA1"

        // 🤒👅💦👽🐖😥🦓🐱⛑🐼👔🤚🦌😀😱🙄👘😞👁👿😞✊😉💨🐑😊🦓🐗💼🐀🧵😳
        private const val CUT_EMOJI_SEQUENCE =
            "\uD83E\uDD12\uD83D\uDC45\uD83D\uDCA6\uD83D\uDC7D\uD83D\uDC16\uD83D\uDE25\uD83E\uDD93\uD83D\uDC31⛑\uD83D\uDC3C\uD83D\uDC54\uD83E\uDD1A\uD83E\uDD8C\uD83D\uDE00\uD83D\uDE31\uD83D\uDE44\uD83D\uDC58\uD83D\uDE1E\uD83D\uDC41\uD83D\uDC7F\uD83D\uDE1E✊\uD83D\uDE09\uD83D\uDCA8\uD83D\uDC11\uD83D\uDE0A\uD83E\uDD93\uD83D\uDC17\uD83D\uDCBC\uD83D\uDC00\uD83E\uDDF5\uD83D\uDE33"
        private const val PUBLIC_KEY =
            "2e93c460DF49D8CFBBF7A06DD9004C25A84F92584F7D0AC5E30BD8E0BEEE9A43"
    }

    class NetworkRepositoryMock : NetworkRepository {
        override var supportedNetworks: List<Network> = listOf(Network.WEATHERWAX)
        override var currentNetwork: TariNetwork? = TariNetwork(Network.WEATHERWAX, "", "")
        override var ffiNetwork: Network? = Network.WEATHERWAX
        override var incompatibleNetworkShown: Boolean = false

        override fun getAllNetworks(): List<TariNetwork> = listOf()

    }
}
