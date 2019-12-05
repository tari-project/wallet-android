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

class Wallet(ptr: WalletPtr) : FFIObjectWrapper(ptr) {

    /**
     * JNI functions.
     */
    private external fun walletGetContactsJNI(walletPtr: WalletPtr): ContactsPtr
    private external fun walletGetPublicKeyJNI(walletPtr: WalletPtr): PublicKeyPtr
    private external fun walletGetAvailableBalanceJNI(walletPtr: WalletPtr): ULong
    private external fun walletGetPendingIncomingBalanceJNI(walletPtr: WalletPtr): ULong
    private external fun walletGetPendingOutgoingBalanceJNI(walletPtr: WalletPtr): ULong
    private external fun walletAddContactJNI(walletPtr: WalletPtr, contactPtr: ContactPtr): Boolean
    private external fun walletRemoveContactJNI(walletPtr: WalletPtr, contactPtr: ContactPtr): Boolean
    private external fun walletDestroyJNI(walletPtr: WalletPtr)

    private external fun walletTestGenerateDataJNI(
        walletPtr: WalletPtr,
        datastorePath: String
    ): Boolean

    companion object {

        /**
         * JNI static functions.
         */
        @JvmStatic
        private external fun walletCreateJNI(
            walletConfigPtr: WalletConfigPtr,
            logPath: String
        ): WalletPtr

        fun create(walletConfig: CommsConfig, logPath: String): Wallet {
            return Wallet(walletCreateJNI(walletConfig.ptr, logPath))
        }

    }

    fun generateTestData(datastorePath: String): Boolean {
        return walletTestGenerateDataJNI(ptr, datastorePath)
    }

    fun getPublicKey(): PublicKey {
        return PublicKey(walletGetPublicKeyJNI(ptr))
    }

    fun getContacts(): Contacts {
        return Contacts(walletGetContactsJNI(ptr))
    }

    fun addContact(contact: Contact): Boolean {
        return walletAddContactJNI(ptr, contact.ptr)
    }

    fun removeContact(contact: Contact): Boolean {
        return walletRemoveContactJNI(ptr, contact.ptr)
    }

    public override fun destroy() {
        walletDestroyJNI(ptr)
        super.destroy()
    }

}