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
 * Pending outbound transaction wrapper.
 *
 * @author Kutsal Kaan Bilgin
 */
class PendingOutboundTransaction(ptr: PendingOutboundTransactionPtr) : FFIObjectWrapper(ptr) {

    /**
     * JNI functions.
     */
    private external fun getIdJNI(ptr: PendingOutboundTransactionPtr): Long
    private external fun getDestinationPublicKeyJNI(ptr: PendingOutboundTransactionPtr): PublicKeyPtr
    private external fun getAmountJNI(ptr: PendingOutboundTransactionPtr): Long
    private external fun getTimestampJNI(ptr: PendingOutboundTransactionPtr): Long
    private external fun getMessageJNI(ptr: PendingOutboundTransactionPtr): String
    private external fun destroyJNI(ptr: PendingOutboundTransactionPtr)

    fun getId(): Long {
        return getIdJNI(ptr)
    }

    fun getDestinationPublicKey(): PublicKey {
        return PublicKey(getDestinationPublicKeyJNI(ptr))
    }

    fun getAmount(): Long {
        return getAmountJNI(ptr)
    }

    fun getTimestamp(): Long {
        return getTimestampJNI(ptr)
    }

    fun getMessage(): String {
        return getMessageJNI(ptr)
    }

    public override fun destroy() {
        destroyJNI(ptr)
        super.destroy()
    }

}
