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

internal typealias FFITransportTypePtr = Long

/**
 * Wrapper for native private key type.
 *
 * @author The Tari Development Team
 */
internal class FFITransportType constructor(pointer: FFITransportTypePtr) : FFIBase() {

    // region JNI
    private external fun jniMemoryTransport()

    private external fun jniGetMemoryAddress(
        libError: FFIError
    ): String

    private external fun jniTCPTransport(
        listenerAddress: String, libError: FFIError
    )

    private external fun jniTorTransport(
        control_server_address: String,
        tor_port: Int,
        tor_cookie: FFIByteVector,
        tor_identity: FFIByteVector,
        socks_username: String,
        socks_password: String,
        libError: FFIError
    )

    private external fun jniDestroy()
    // endregion

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    constructor() : this(nullptr) {
        jniMemoryTransport()
    }

    constructor(listenerAddress: NetAddressString) : this(nullptr) {
        val error = FFIError()
        jniTCPTransport(listenerAddress.toString(), error)
        throwIf(error)
    }

    constructor(
        controlAddress: NetAddressString,
        torPort: Int,
        torCookie: FFIByteVector,
        torIdentity: FFIByteVector,
        socksUsername: String,
        socksPassword: String
    ) : this(nullptr) {
        val error = FFIError()
        jniTorTransport(
            controlAddress.toString(),
            torPort,
            torCookie,
            torIdentity,
            socksUsername,
            socksPassword,
            error
        )
        throwIf(error)
    }
    fun getPointer(): FFIPrivateKeyPtr {
        return ptr
    }

    fun getAddress(): String {
        val error = FFIError()
        val result = jniGetMemoryAddress(error)
        throwIf(error)
        return result
    }

    override fun destroy() {
        jniDestroy()
    }

}