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

/**
 * Parses a deep link and contains the structured deep link details.
 *
 * @author The Tari Development Team
 */
internal class DeepLink private constructor(val network: Network, val type: Type) {

    /**
     * Deep link type.
     * Emoji id example: tari://rincewind/eid/🤒👅💦👽🐖😥🦓🐱⛑🐼👔🤚🦌😀😱🙄👘😞👁👿😞✊😉💨🐑😊🦓🐗💼🐀🧵😳🦡
     * (chunked: 🤒👅💦|👽🐖😥|🦓🐱⛑|🐼👔🤚|🦌😀😱|🙄👘😞|👁👿😞|✊😉💨|🐑😊🦓|🐗💼🐀|🧵😳🦡)
     * Public key example: tari://rincewind/pubkey/2e93c460DF49D8CFBBF7A06DD9004C25A84F92584F7D0AC5E30BD8E0BEEE9A43
     */
    enum class Type(val uriComponent: String) {
        EMOJI_ID("eid"),
        PUBLIC_KEY_HEX("pubkey");

        /**
         * Value is an emoji id or public key hex string.
         */
        lateinit var value: String
    }

    companion object {

        private val regexNetwork =
            "(" + Network.MAINNET.uriComponent + "|" + Network.TESTNET_1.uriComponent + ")"
        private val regexEmojiId = "(" + Type.EMOJI_ID.uriComponent + ")/(.{33})"
        private val regexPublicKeyHex =
            "(" + Type.PUBLIC_KEY_HEX.uriComponent + ")/([a-zA-Z0-9]{64})"
        private val emojiIdRegex = Regex("tari://$regexNetwork/$regexEmojiId")
        private val publicKeyRegex = Regex("tari://$regexNetwork/$regexPublicKeyHex")

        /**
         * Parse deep link.
         *
         * @return null if deep link is not in valid format
         */
        fun from(deepLink: String): DeepLink? {
            if (emojiIdRegex.matches(deepLink)) {
                val matchResult = emojiIdRegex.find(deepLink)
                val (networkUriComponent, _, value) = matchResult!!.destructured
                val type = Type.EMOJI_ID
                type.value = value
                return DeepLink(
                    Network.from(networkUriComponent),
                    type
                )
            } else if (publicKeyRegex.matches(deepLink)) {
                val matchResult = publicKeyRegex.find(deepLink)
                val (networkUriComponent, _, value) = matchResult!!.destructured
                val type = Type.PUBLIC_KEY_HEX
                type.value = value
                return DeepLink(
                    Network.from(networkUriComponent),
                    type
                )
            }
            return null
        }

    }

}