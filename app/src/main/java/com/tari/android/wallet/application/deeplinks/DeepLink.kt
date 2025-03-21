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
package com.tari.android.wallet.application.deeplinks

import android.os.Parcelable
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.ffi.FFISeedWords
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.Base58
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.extension.parseToBigInteger
import kotlinx.parcelize.Parcelize

/**
 * Parses a deep link and contains the structured deep link details.
 *
 * @author The Tari Development Team
 */
sealed class DeepLink : Parcelable {

    open fun getParams(): Map<String, String> = emptyMap()
    open fun getCommand(): String = ""

    // tari://esmeralda/contacts?list[0][alias]=Name&list[0][tariAddress]=tariAddress&list[1][alias]=Name&list[1][tariAddress]=tariAddress
    @Parcelize
    data class Contacts(val contacts: List<DeeplinkContact>) : DeepLink() {

        constructor(params: Map<String, String>) : this(
            contacts = params.filterKeys { it.startsWith("list[") }
                .map { FormatExtractor(it.key, it.value) }
                .groupBy { it.index }
                .map { param ->
                    DeeplinkContact(
                        alias = param.value.firstOrNull { it.name == KEY_ALIAS }?.value.orEmpty(),
                        tariAddress = param.value.firstOrNull { it.name == KEY_TARI_ADDRESS }?.value.orEmpty()
                    )
                }
                .filter { TariWalletAddress.validateBase58(it.tariAddress) },
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            contacts.forEachIndexed { index, contact ->
                put("list[$index][$KEY_ALIAS]", contact.alias)
                put("list[$index][$KEY_TARI_ADDRESS]", contact.tariAddress)
            }
        }

        override fun getCommand(): String = COMMAND_CONTACTS

        companion object {
            const val COMMAND_CONTACTS = "contacts"
            const val KEY_ALIAS = "alias"
            const val KEY_TARI_ADDRESS = "tariAddress"
        }

        @Parcelize
        data class DeeplinkContact(val alias: String, val tariAddress: Base58) : Parcelable

        class FormatExtractor(val key: String, val value: String = "") {
            val index: Int
            val name: String

            init {
                key.replace("list[", "")
                    .replace("]", "")
                    .split("[")
                    .let {
                        index = it[0].toInt()
                        name = it[1].split("=")[0]
                    }
            }
        }
    }

    @Parcelize
    data class Send(val walletAddress: Base58 = "", val amount: MicroTari? = null, val note: String = "") : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[KEY_TARI_ADDRESS].orEmpty(),
            params[KEY_AMOUNT]?.let { if (it.isEmpty()) null else MicroTari(it.parseToBigInteger()) },
            params[KEY_NOTE].orEmpty()
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(KEY_TARI_ADDRESS, walletAddress)
            put(KEY_AMOUNT, amount?.formattedValue.orEmpty())
            put(KEY_NOTE, note)
        }

        override fun getCommand(): String = COMMAND_SEND

        companion object {
            const val COMMAND_SEND = "transactions/send"
            const val KEY_TARI_ADDRESS = "tariAddress"
            const val KEY_AMOUNT = "amount"
            const val KEY_NOTE = "note"
        }
    }

    @Parcelize
    data class UserProfile(val tariAddress: Base58 = "", val alias: String = "") : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[KEY_WALLET_ADDRESS].orEmpty(),
            params[KEY_ALIAS].orEmpty(),
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(KEY_WALLET_ADDRESS, tariAddress)
            put(KEY_ALIAS, alias)
        }

        override fun getCommand(): String = COMMAND_PROFILE

        companion object {
            const val COMMAND_PROFILE = "profile"
            const val KEY_WALLET_ADDRESS = "tariAddress"
            const val KEY_ALIAS = "alias"
        }
    }

    @Parcelize
    data class AddBaseNode(val name: String = "", val peer: String = "") : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[KEY_NAME].orEmpty(),
            params[KEY_PEER].orEmpty(),
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(KEY_NAME, name)
            put(KEY_PEER, peer)
        }

        override fun getCommand(): String = COMMAND_ADD_NODE

        companion object {
            const val COMMAND_ADD_NODE = "base_nodes/add"
            const val KEY_NAME = "name"
            const val KEY_PEER = "peer"
        }
    }

    @Parcelize
    data class TorBridges(val torConfigurations: List<TorBridgeConfiguration>) : DeepLink()

    // tari://esmeralda/paper_wallet?private_key=1234567890XX&anon_id=1234567890XX&balance=0%20%C2%B5T&tt=1234567890XX
    @Parcelize
    data class PaperWallet(
        val privateKey: String,
        val anonId: String = "",
        val balance: String = "",
        val tt: String = "",
    ) : DeepLink() {

        fun seedWords(passphrase: String): List<String>? = runCatching {
            FFISeedWords(this.privateKey, passphrase).runWithDestroy { seedWords -> (0 until seedWords.getLength()).map { seedWords.getAt(it) } }
        }.getOrNull()

        constructor(params: Map<String, String>) : this(
            params[KEY_PRIVATE_KEY].orEmpty(),
            params[KEY_ANON_ID].orEmpty(),
            params[KEY_BALANCE].orEmpty(),
            params[KEY_TT].orEmpty(),
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(KEY_PRIVATE_KEY, privateKey)
            put(KEY_ANON_ID, anonId)
            put(KEY_BALANCE, balance)
            put(KEY_TT, tt)
        }

        override fun getCommand(): String = COMMAND_PAPER_WALLET

        companion object {
            const val COMMAND_PAPER_WALLET = "paper_wallet"
            const val KEY_PRIVATE_KEY = "private_key"
            const val KEY_ANON_ID = "anon_id"
            const val KEY_BALANCE = "balance"
            const val KEY_TT = "tt"
        }
    }

    // tari://nextnet/airdrop/auth?token=XXXXX&refreshToken=XXXXX
    @Parcelize
    data class AirdropLoginToken(
        val token: String,
        val refreshToken: String,
    ) : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[KEY_TOKEN].orEmpty(),
            params[KEY_REFRESH_TOKEN].orEmpty(),
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(KEY_TOKEN, token)
            put(KEY_REFRESH_TOKEN, refreshToken)
        }

        override fun getCommand(): String = COMMAND_AIRDROP_LOGIN

        companion object {
            const val COMMAND_AIRDROP_LOGIN = "airdrop/auth"
            const val KEY_TOKEN = "token"
            const val KEY_REFRESH_TOKEN = "refreshToken"
        }
    }

    companion object {

        fun getByCommand(command: String, params: Map<String, String>): DeepLink? = when (command) {
            Contacts.COMMAND_CONTACTS -> Contacts(params)
            Send.COMMAND_SEND -> Send(params)
            AddBaseNode.COMMAND_ADD_NODE -> AddBaseNode(params)
            UserProfile.COMMAND_PROFILE -> UserProfile(params)
            PaperWallet.COMMAND_PAPER_WALLET -> PaperWallet(params)
            AirdropLoginToken.COMMAND_AIRDROP_LOGIN -> AirdropLoginToken(params)
            else -> null
        }
    }
}