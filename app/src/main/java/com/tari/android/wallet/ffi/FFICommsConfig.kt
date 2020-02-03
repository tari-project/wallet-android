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

import android.util.Log
import java.io.File
import java.security.InvalidParameterException

internal typealias FFICommsConfigPtr = Long

/**
 * Tari comms config wrapper.
 *
 * @author The Tari Development Team
 */
internal class FFICommsConfig constructor(pointer: FFICommsConfigPtr): FFIBase() {

    // region JNI

    private external fun jniDestroy(commsConfigPtr: FFICommsConfigPtr)
    private external fun jniCreate(
        controlServiceAddress: String,
        listenerAddress: String,
        databaseName: String,
        datastorePath: String,
        privateKeyPtr: FFIPrivateKeyPtr,
        error: FFIError
    ): FFICommsConfigPtr

    // endregion

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    constructor(
        controlServiceAddress: NetAddressString,
        listenerAddress: NetAddressString,
        databaseName: String,
        datastorePath: String,
        privateKey: FFIPrivateKey
    ) : this(nullptr) {
        if (databaseName.isEmpty()) {
            throw InvalidParameterException("databaseName may not be empty")
        }
        val writeableDir = File(datastorePath)
        if (writeableDir.exists() && writeableDir.isDirectory && writeableDir.canWrite()) {
            val error = FFIError()
            ptr = jniCreate(
                controlServiceAddress.toString(),
                listenerAddress.toString(),
                databaseName,
                datastorePath,
                privateKey.getPointer(),
                error
            )
            if (error.code != 0) {
                throw RuntimeException()
            }
        } else {
            if (!writeableDir.exists()) {
                Log.i("Directory", "Doesn't exist")
            } else if (!writeableDir.isDirectory) {
                Log.i("Directory", "Isn't a directory")
            } else if (!writeableDir.canWrite()) {
                Log.i("Directory", "Permission problem")
            }
            throw FileSystemException(writeableDir)
        }
    }

    fun getPointer(): FFICommsConfigPtr {
        return ptr
    }

    override fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}