/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
package com.tari.android.wallet.ffi

/**
 * Wallet wrapper.
 *
 * @author Kutsal Kaan Bilgin
 */
open class Wallet(ptr: WalletPtr) : FFIObjectWrapper(ptr) {

    /**
     * JNI functions.
     */
    private external fun getPublicKeyJNI(walletPtr: WalletPtr): PublicKeyPtr
    private external fun getAvailableBalanceJNI(walletPtr: WalletPtr): Long
    private external fun getPendingIncomingBalanceJNI(walletPtr: WalletPtr): Long
    private external fun getPendingOutgoingBalanceJNI(walletPtr: WalletPtr): Long
    private external fun getContactsJNI(walletPtr: WalletPtr): ContactsPtr
    private external fun addContactJNI(walletPtr: WalletPtr, contactPtr: ContactPtr): Boolean
    private external fun removeContactJNI(walletPtr: WalletPtr, contactPtr: ContactPtr): Boolean
    private external fun getCompletedTransactionsJNI(walletPtr: WalletPtr): CompletedTransactionsPtr
    private external fun getCompletedTransactionByIdJNI(
        walletPtr: WalletPtr,
        id: Long
    ): CompletedTransactionPtr

    private external fun getPendingOutboundTransactionsJNI(walletPtr: WalletPtr): PendingOutboundTransactionsPtr
    private external fun getPendingOutboundTransactionByIdJNI(
        walletPtr: WalletPtr,
        id: Long
    ): PendingOutboundTransactionPtr

    private external fun getPendingInboundTransactionsJNI(walletPtr: WalletPtr): PendingInboundTransactionsPtr
    private external fun getPendingInboundTransactionByIdJNI(
        walletPtr: WalletPtr,
        id: Long
    ): PendingInboundTransactionPtr

    private external fun destroyJNI(walletPtr: WalletPtr)

    /**
     * JNI callback registration functions.
     */
    private external fun registerTransactionBroadcastListenerJNI(
        walletPtr: WalletPtr,
        listener: OnTransactionBroadcastListener
    ): Boolean
    private external fun registerTransactionMinedListenerJNI(
        walletPtr: WalletPtr,
        listener: OnTransactionMinedListener
    ): Boolean
    private external fun registerTransactionReceivedListenerJNI(
        walletPtr: WalletPtr,
        listener: OnTransactionReceivedListener
    ): Boolean
    private external fun registerTransactionReplyReceivedListenerJNI(
        walletPtr: WalletPtr,
        listener: OnTransactionReplyReceivedListener
    ): Boolean

    companion object {

        /**
         * JNI static functions.
         */
        @JvmStatic
        external fun createJNI(
            walletConfigPtr: WalletConfigPtr,
            logPath: String
        ): WalletPtr

        internal fun create(walletConfig: CommsConfig, logPath: String): Wallet {
            return Wallet(createJNI(walletConfig.ptr, logPath))
        }

    }

    fun getAvailableBalance(): Long {
        return getAvailableBalanceJNI(ptr)
    }

    fun getPendingIncomingBalance(): Long {
        return getPendingIncomingBalanceJNI(ptr)
    }

    fun getPendingOutgoingBalance(): Long {
        return getPendingOutgoingBalanceJNI(ptr)
    }

    fun getPublicKey(): PublicKey {
        return PublicKey(getPublicKeyJNI(ptr))
    }

    fun getContacts(): Contacts {
        return Contacts(getContactsJNI(ptr))
    }

    fun addContact(contact: Contact): Boolean {
        return addContactJNI(ptr, contact.ptr)
    }

    fun removeContact(contact: Contact): Boolean {
        return removeContactJNI(ptr, contact.ptr)
    }

    fun getCompletedTransactions(): CompletedTransactions {
        return CompletedTransactions(getCompletedTransactionsJNI(ptr))
    }

    fun getCompletedTransactionById(id: Long): CompletedTransaction {
        return CompletedTransaction(getCompletedTransactionByIdJNI(ptr, id))
    }

    fun getPendingOutboundTransactions(): PendingOutboundTransactions {
        return PendingOutboundTransactions(getPendingOutboundTransactionsJNI(ptr))
    }

    fun getPendingOutboundTransactionById(id: Long): PendingOutboundTransaction {
        return PendingOutboundTransaction(getPendingOutboundTransactionByIdJNI(ptr, id))
    }

    fun getPendingInboundTransactions(): PendingInboundTransactions {
        return PendingInboundTransactions(getPendingInboundTransactionsJNI(ptr))
    }

    fun getPendingInboundTransactionById(id: Long): PendingInboundTransaction {
        return PendingInboundTransaction(getPendingInboundTransactionByIdJNI(ptr, id))
    }

    fun setOnTransactionBroadcastListener(listener: OnTransactionBroadcastListener): Boolean {
        return registerTransactionBroadcastListenerJNI(ptr, listener)
    }

    fun setOnTransactionMinedListener(listener: OnTransactionMinedListener): Boolean {
        return registerTransactionMinedListenerJNI(ptr, listener)
    }

    fun setOnTransactionReceivedListener(listener: OnTransactionReceivedListener): Boolean {
        return registerTransactionReceivedListenerJNI(ptr, listener)
    }

    fun setOnTransactionReplyReceivedListener(listener: OnTransactionReplyReceivedListener): Boolean {
        return registerTransactionReplyReceivedListenerJNI(ptr, listener)
    }

    public override fun destroy() {
        destroyJNI(ptr)
        super.destroy()
    }

}