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

import com.tari.android.wallet.ffi.FFITxStatus

/**
 * Transaction status.
 *
 * @author The Tari Development Team
 */
enum class TxStatus {
    TX_NULL_ERROR,
    COMPLETED,
    BROADCAST,
    MINED_UNCONFIRMED,
    IMPORTED,
    PENDING,
    COINBASE,
    MINED_CONFIRMED,
    REJECTED,
    FAUX_UNCONFIRMED,
    FAUX_CONFIRMED,
    QUEUED,
    COINBASE_UNCONFIRMED,
    COINBASE_CONFIRMED,
    UNKNOWN;
    
    companion object {
        fun map(ffiStatus: FFITxStatus): TxStatus {
            return when (ffiStatus) {
                FFITxStatus.TX_NULL_ERROR -> TX_NULL_ERROR
                FFITxStatus.COMPLETED -> COMPLETED
                FFITxStatus.BROADCAST -> BROADCAST
                FFITxStatus.MINED_UNCONFIRMED -> MINED_UNCONFIRMED
                FFITxStatus.IMPORTED -> IMPORTED
                FFITxStatus.PENDING -> PENDING
                FFITxStatus.COINBASE -> COINBASE
                FFITxStatus.MINED_CONFIRMED -> MINED_CONFIRMED
                FFITxStatus.REJECTED -> REJECTED
                FFITxStatus.FAUX_CONFIRMED -> FAUX_CONFIRMED
                FFITxStatus.FAUX_UNCONFIRMED -> FAUX_UNCONFIRMED
                FFITxStatus.QUEUED -> QUEUED
                FFITxStatus.COINBASE_UNCONFIRMED -> COINBASE_UNCONFIRMED
                FFITxStatus.COINBASE_CONFIRMED -> COINBASE_CONFIRMED
                else -> UNKNOWN
            }
        }
    }
}