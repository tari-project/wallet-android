/**
 * Copyright 2019 The Tari Project
 *
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
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
package com.tari.android.wallet

import android.util.Log
import com.tari.android.wallet.ffi.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.math.BigInteger


class WalletTests {
    class testListener: WalletListenerAdapter
    {
        override fun onTransactionBroadcast(tx: CompletedTransaction) {
            Log.i("ID",tx.getId().toString())
        }

        override fun onTransactionReceived(tx: PendingInboundTransaction) {
            Log.i("ID",tx.getId().toString())
        }

        override fun onTransactionReplyReceived(tx: CompletedTransaction) {
            Log.i("ID",tx.getId().toString())
        }

        override fun onTransactionMined(tx: CompletedTransaction) {
            Log.i("ID",tx.getId().toString())
        }

        override fun onTransactionFinalized(tx: CompletedTransaction) {
            Log.i("ID",tx.getId().toString())
        }
    }

    @Test
    fun testWallet() {
        TestUtil.clearTestFiles(StringBuilder().append(TestUtil.WALLET_DATASTORE_PATH).toString())
        Log.i("Datastore_Path",TestUtil.WALLET_DATASTORE_PATH)
        val m = TestUtil.clearTestFiles(TestUtil.WALLET_DATASTORE_PATH)
        if (!m)
        {
            throw RuntimeException()
        }
        val commsConfig = CommsConfig(TestUtil.WALLET_CONTROL_SERVICE_ADDRESS,
            TestUtil.WALLET_LISTENER_ADDRESS,
            TestUtil.WALLET_DB_NAME,
            TestUtil.WALLET_DATASTORE_PATH,
            PrivateKey(HexString(TestUtil.PRIVATE_KEY_HEX_STRING))
        )
        val listeners = testListener()
        val wallet = Wallet
        wallet.init(commsConfig,TestUtil.WALLET_LOG_FILE_PATH,listeners)
        assertTrue(wallet.getPointer() != nullptr)
        commsConfig.destroy()
        //test get public key
        var pk = wallet.getPublicKey()
        assertTrue(pk.getPointer() != nullptr)
        //test data generation
        assertTrue(wallet.generateTestData(TestUtil.WALLET_DATASTORE_PATH))
        //test contacts
        var contacts = wallet.getContacts()
        assertTrue(contacts.getPointer() != nullptr)
        var length = contacts.getLength()
        assertTrue(length > 0)
        var contact = contacts.getAt(0)
        assertTrue(contact.getPointer() != nullptr)
        var removed = wallet.removeContact(contact)
        assertTrue(removed)
        var added = wallet.addContact(contact)
        assertTrue(added)
        //test completed transactions
        var completedTansactions = wallet.getCompletedTransactions()
        assertTrue(completedTansactions.getPointer() != nullptr)
        assertTrue(completedTansactions.getLength() > 0)
        var completed: CompletedTransaction? = null
        for ( i in 0..completedTansactions.getLength())
        {
            completed = completedTansactions.getAt(0)
            assertTrue(completed.getPointer() != nullptr)
            if (completed.getStatus() == CompletedTransaction.Status.MINED)
            {
                completed.destroy()
            } else
            {
                break
            }
        }
        val completedID = completed!!.getId()
        val completedSource = completed!!.getSourcePublicKey()
        assertTrue(completedSource.getPointer() != nullptr)
        completedSource.destroy()
        val completedDestination = completed!!.getDestinationPublicKey()
        assertTrue(completedDestination.getPointer() != nullptr)
        completedDestination.destroy()
        val completedAmount = completed!!.getAmount()
        assertTrue(completedAmount > BigInteger("0"))
        val completedFee = completed!!.getFee()
        assertTrue(completedFee > BigInteger("0"))
        val completedTimestamp = completed!!.getTimestamp()
        val completedMessage = completed!!.getMessage()
        completed?.destroy()
        completed = wallet.getCompletedTransactionById(completedID!!.toLong())
        assertTrue(completed.getPointer() != nullptr)
        assertTrue(wallet.minedCompletedTransaction(completed))
        completed.destroy()
        completedTansactions.destroy()
        //test pending inbound transactions
        assertTrue(wallet.receiveTransaction())
        var pendingInboundTransactions = wallet.getPendingInboundTransactions()
        assertTrue(pendingInboundTransactions.getPointer() != nullptr)
        assertTrue(pendingInboundTransactions.getLength() > 0)
        var inbound = pendingInboundTransactions.getAt(0)
        assertTrue(inbound.getPointer() != nullptr)
        val inboundID = inbound.getId()
        val inboundSource = inbound.getSourcePublicKey()
        assertTrue(inboundSource.getPointer() != nullptr)
        val inboundAmount = inbound.getAmount()
        assertTrue(inboundAmount > BigInteger("0"))
        val inboundTimestamp = inbound.getTimestamp()
        val inboundMessage = inbound.getMessage()
        inbound.destroy()
        inbound = wallet.getPendingInboundTransactionById(inboundID.toLong())
        assertTrue(inbound.getPointer() != nullptr)
        assertTrue(wallet.transactionBroadcast(inbound))
        inbound.destroy()
        pendingInboundTransactions.destroy()
        //test pending outbound transactions
        var pendingOutboundTransactions = wallet.getPendingOutboundTransactions()
        assertTrue(pendingOutboundTransactions.getPointer() != nullptr)
        assertTrue(pendingOutboundTransactions.getLength() > 0)
        var outbound = pendingOutboundTransactions.getAt(0)
        assertTrue(outbound.getPointer() != nullptr)
        val outboundID = outbound.getId()
        val outboundSource = outbound.getDestinationPublicKey()
        assertTrue(outboundSource.getPointer() != nullptr)
        val outboundAmount = outbound.getAmount()
        assertTrue(outboundAmount > BigInteger("0"))
        val outboundTimestamp = outbound.getTimestamp()
        val outboundMessage = outbound.getMessage()
        outbound.destroy()
        outbound = wallet.getPendingOutboundTransactionById(outboundID.toLong())
        assertTrue(outbound.getPointer() != nullptr)
        assertTrue(wallet.completeSentTransaction(outbound))
        outbound.destroy()
        pendingOutboundTransactions.destroy()
        //test balances
        val available = wallet.getAvailableBalance()
        assertTrue(available > BigInteger("0"))
        val pendingIn = wallet.getPendingIncomingBalance()
        assertTrue(pendingIn > BigInteger("0"))
        val pendingOut = wallet.getPendingOutgoingBalance()
        assertTrue(pendingOut == BigInteger("0"))
        wallet.destroy()

        //TODO test listeners
    }

}