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
package com.tari.android.wallet.data.sharedPrefs.tor

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorPrefRepository @Inject constructor(sharedPrefs: SharedPreferences, networkRepository: NetworkPrefRepository) :
    CommonPrefRepository(networkRepository) {

    private object Key {
        const val CURRENT_TOR_BRIDGE = "tari_current_tor_bridge"
        const val CUSTOM_TOR_BRIDGES = "tari_custom_tor_bridges"
        const val TOR_BIN_PATH = "tari_wallet_tor_bin_path"
        const val TORRC_BIN_PATH = "tari_wallet_torrc_bin_path"
    }

    var currentTorBridges: TorBridgeConfigurationList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.CURRENT_TOR_BRIDGE),
        type = TorBridgeConfigurationList::class.java,
        defValue = TorBridgeConfigurationList(),
    )

    var customTorBridges: TorBridgeConfigurationList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.CUSTOM_TOR_BRIDGES),
        type = TorBridgeConfigurationList::class.java,
        defValue = TorBridgeConfigurationList(),
    )

    var torBinPath: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.TOR_BIN_PATH))

    var torrcBinPath: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.TORRC_BIN_PATH))

    fun clear() {
        currentTorBridges = TorBridgeConfigurationList()
        customTorBridges = TorBridgeConfigurationList()
        torBinPath = null
    }

    fun addTorBridgeConfiguration(torBridgeConfiguration: TorBridgeConfiguration) {
        customTorBridges.apply {
            add(torBridgeConfiguration)
            customTorBridges = TorBridgeConfigurationList(this.distinct())
        }
    }

    fun addCurrentTorBridge(torBridgeConfiguration: TorBridgeConfiguration) {
        currentTorBridges.apply {
            add(torBridgeConfiguration)
            currentTorBridges = TorBridgeConfigurationList(this.distinct())
        }
    }

    fun removeCurrentTorBridge(torBridgeConfiguration: TorBridgeConfiguration) {
        currentTorBridges.apply {
            remove(torBridgeConfiguration)
            currentTorBridges = TorBridgeConfigurationList(this.distinct())
        }
    }
}

class TorBridgeConfigurationList(configs: List<TorBridgeConfiguration>) : ArrayList<TorBridgeConfiguration>(configs) {
    constructor() : this(emptyList())
}