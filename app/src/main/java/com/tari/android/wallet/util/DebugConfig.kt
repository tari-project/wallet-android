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
@file:Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName", "KotlinConstantConditions", "SameParameterValue")

package com.tari.android.wallet.util

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import org.joda.time.DateTime
import yat.android.lib.YatIntegration
import java.math.BigInteger
import kotlin.random.Random

/**
 *  Constants used for developing and debugging.
 */
object DebugConfig {

    private const val _mockedTurned = false
    val mockedDataEnabled = _mockedTurned && isDebug() // TODO split this flag to multiple different types of mocked data

    val mockUtxos = valueIfDebug(true)

    val mockTxs = valueIfDebug(false)

    const val isChatEnabled = false

    private const val _useYatSandbox = false
    val yatEnvironment = if (_useYatSandbox && isDebug()) YatEnvironment.SANDBOX else YatEnvironment.PRODUCTION

    private const val _mockNetwork = false
    val mockNetwork = _mockNetwork && isDebug()

    private const val _hardcodedBaseNode = false
    val hardcodedBaseNodes = _hardcodedBaseNode && isDebug()

    private fun isDebug() = BuildConfig.BUILD_TYPE == "debug"

    private fun valueIfDebug(value: Boolean) = isDebug() && value
}

object MockDataStub {
    private const val EMOJI_ID =
        "\uD83C\uDF34\uD83C\uDF0D\uD83C\uDFB5\uD83C\uDFBA\uD83D\uDDFD\uD83C\uDF37\uD83D\uDE91\uD83C\uDF45\uD83D\uDC60\uD83C\uDF1F\uD83D\uDC8C\uD83D\uDE97\uD83D\uDC40\uD83D\uDD29\uD83C\uDF08\uD83D\uDC1D\uD83C\uDF37\uD83C\uDF70\uD83C\uDF38\uD83C\uDF81\uD83C\uDF55\uD83D\uDEBF\uD83D\uDC34\uD83D\uDCA6\uD83D\uDE0E\uD83D\uDEAA\uD83C\uDFE0\uD83D\uDD29\uD83C\uDFE0\uD83D\uDE82\uD83C\uDFBA\uD83C\uDFC6\uD83C\uDFB3"
    private const val HEX = "C05575BE00EF016A209B1F493D9027B0E330F3E25FE89BBE6FA66D966EE5B6356"

    private val EMOJI_ID_ZERO = (0..25).map { "\uD83C\uDF00" }.joinToString("")
    private const val HEX_ZERO = TariWalletAddress.HEX_ZERO_66

    val WALLET_ADDRESS = TariWalletAddress(hexString = HEX, emojiId = EMOJI_ID)
    val WALLET_ADDRESS_ZERO = TariWalletAddress(HEX_ZERO, EMOJI_ID_ZERO)

    fun createUtxoList() = List(20) { UtxosViewHolderItem(createUtxo()) }

    fun createUtxo() = TariUtxo(
        value = MicroTari(BigInteger.valueOf(Random.nextLong(1, 100000) * 10000)),
        status = TariUtxo.UtxoStatus.entries.toTypedArray()[Random.nextInt(0, 3)],
        timestamp = DateTime.now().toDate().time,
        minedHeight = 12243,
        commitment = "Mocked Tari UTXO!!!",
    )

    fun createTxList(
        contactsRepository: ContactsRepository,
        gifRepository: GIFRepository,
        confirmationCount: Long,
        title: String = "Mocked Transactions"
    ) = listOf(
        TitleViewHolderItem(title = title, isFirst = true),
        createTx(
            contactsRepository, gifRepository, confirmationCount,
            amount = 1100000,
            contactAlias = "Alice",
        ),
        createTx(
            contactsRepository, gifRepository, confirmationCount,
            amount = 1200000,
            contactAlias = "Bob",
        ),
        createTx(
            contactsRepository, gifRepository, confirmationCount,
            amount = 1300000,
            contactAlias = "Charlie",
        ),
    )

    fun createTx(
        contactsRepository: ContactsRepository,
        gifRepository: GIFRepository,
        confirmationCount: Long,
        amount: Long = 100000,
        contactAlias: String = "Test",
    ) = TransactionItem(
        tx = CompletedTx(
            direction = Tx.Direction.INBOUND,
            status = TxStatus.MINED_CONFIRMED,
            amount = MicroTari(BigInteger.valueOf(amount)),
            fee = MicroTari(BigInteger.valueOf(1000)),
            message = "https://giphy.com/embed/5885nYOgBHdCw",
            timestamp = BigInteger.valueOf(System.currentTimeMillis()),
            id = BigInteger.valueOf(1),
            tariContact = TariContact(WALLET_ADDRESS_ZERO, contactAlias),
        ),
        contact = contactsRepository.ffiBridge.getContactForTx(CompletedTx()),
        position = 0,
        viewModel = GIFViewModel(gifRepository),
        requiredConfirmationCount = confirmationCount,
    )
}

object YatEnvironment {
    val SANDBOX = YatIntegration.Environment("https://a.yat.fyi/", "https://yat.fyi/")
    val PRODUCTION = YatIntegration.Environment("https://a.y.at/", "https://y.at/")
}