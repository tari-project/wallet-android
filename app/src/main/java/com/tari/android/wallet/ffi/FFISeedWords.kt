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

import com.tari.android.wallet.model.Base58
import com.tari.android.wallet.model.seedPhrase.SeedWordsWordPushResult

/**
 * Wrapper for native private key type.
 *
 * @author The Tari Development Team
 */
class FFISeedWords() : FFIBase() {

    private external fun jniCreate()
    private external fun jniFromBase58(cypher: String, passphrase: String, libError: FFIError)
    private external fun jniPushWord(word: String, libError: FFIError): Int
    private external fun jniGetLength(libError: FFIError): Int
    private external fun jniGetAt(index: Int, libError: FFIError): String
    private external fun jniDestroy()

    external fun jniGetMnemonicWordListForLanguage(language: String, libError: FFIError)

    init {
        jniCreate()
    }

    constructor(base58: Base58, passphrase: String) : this() {
        runWithError { jniFromBase58(base58, passphrase, it) }
    }

    constructor(pointer: FFIPointer) : this() {
        if (pointer.isNull()) error("Pointer must not be null")
        this.pointer = pointer
    }

    fun getLength(): Int = runWithError { jniGetLength(it) }

    fun getAt(index: Int): String = runWithError { jniGetAt(index, it) }

    fun pushWord(word: String): SeedWordsWordPushResult = runWithError { SeedWordsWordPushResult.fromInt(jniPushWord(word, it)) }

    override fun destroy() = jniDestroy()

    companion object {
        fun getMnemomicWordList(language: Language): FFISeedWords = FFISeedWords().apply {
            jniGetMnemonicWordListForLanguage(language.name, FFIError())
        }
    }

    enum class Language {
        English,
        Spanish
    }
}

