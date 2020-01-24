/**
 * Copyright 2020 The Tari Project
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
package com.tari.android.wallet.util

import com.tari.android.wallet.ffi.NetAddressString

/**
 * Contains application-wide constant values.
 *
 * @author The Tari Development Team
 */
internal object Constants {

    /**
     * UI constants.
     */
    object UI {
        const val shortAnimDurationMs = 300L
        const val mediumAnimDurationMs = 600L

        object Home {
            const val startupAnimDuration = 1500L
            const val swipeRefreshDummyDuration = 1300L
            const val digitAnimDurationMs = 700L
            const val digitShrinkExpandAnimDurationMs = 200L
        }
    }

    /**
     * Wallet constants.
     */
    object Wallet {
        const val WALLET_DB_NAME: String = "tari_wallet_db"
        internal val WALLET_CONTROL_SERVICE_ADDRESS: NetAddressString = NetAddressString("127.0.0.1", 80)
        internal val WALLET_LISTENER_ADDRESS: NetAddressString = NetAddressString("0.0.0.0", 0)
    }

}