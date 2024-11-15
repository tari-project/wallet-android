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
package com.tari.android.wallet.ffi

/**
 * Tari contact wrapper.
 *
 * @author The Tari Development Team
 */
class FFIContact() : FFIBase() {

    private external fun jniGetAlias(libError: FFIError): String

    private external fun jniGetIsFavorite(libError: FFIError): Boolean
    private external fun jniGetTariWalletAddress(libError: FFIError): FFIPointer
    private external fun jniDestroy()
    private external fun jniCreate(alias: String, isFavorite: Boolean, publicKeyPtr: FFITariWalletAddress, libError: FFIError)

    constructor(pointer: FFIPointer) : this() {
        if (pointer.isNull()) error("Pointer must not be null")
        this.pointer = pointer
    }

    constructor(alias: String, ffiTariWalletAddress: FFITariWalletAddress, isFavorite: Boolean = false) : this() {
        if (alias.isNotEmpty()) {
            runWithError { jniCreate(alias, isFavorite, ffiTariWalletAddress, it) }
        } else {
            throw FFIException(message = "Alias is an empty String.")
        }
    }

    fun getAlias(): String = runWithError { jniGetAlias(it) }

    fun getWalletAddress(): FFITariWalletAddress = runWithError { FFITariWalletAddress(jniGetTariWalletAddress(it)) }

    fun getIsFavorite(): Boolean = runWithError { jniGetIsFavorite(it) }

    override fun toString(): String = "${getAlias()}|${getWalletAddress()}"

    override fun destroy() = jniDestroy()
}