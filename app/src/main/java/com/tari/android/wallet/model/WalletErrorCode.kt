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
package com.tari.android.wallet.model

/**
 * Enumerates FFI error codes.
 *
 * @author The Tari Development Team
 */
enum class WalletErrorCode(val code: Int) {

    NO_ERROR(0),
    /**
     * For any error with an unknown error code, or a different class than FFIError.
     */
    UNKNOWN_ERROR(1000000),

    // TODO The rest will be completed once the error codes get updated in the Rust codebase.
    // https://github.com/tari-project/tari/blob/development/base_layer/wallet_ffi/src/error.rs
    NULL_ERROR(1),
    ALLOCATION_ERROR(2),
    POSITION_INVALID_ERROR(3),
    TOKIO_ERROR(3),

    NOT_ENOUGH_FUNDS(101),
    INCOMPLETE_TX(102),
    DUPLICATE_OUTPUT(103),
    VALUES_NOT_FOUND(104),
    OUTPUT_ALREADY_SPENT(105),
    PENDING_TX_NOT_FOUND(106),

    OUTBOUND_SEND_DISCOVERY_IN_PROGRESS(210);

    companion object {

        fun fromCode(code: Int): WalletErrorCode {
            for (value in values()) {
                if (value.code == code) {
                    return value
                }
            }
            return UNKNOWN_ERROR
        }

    }


}