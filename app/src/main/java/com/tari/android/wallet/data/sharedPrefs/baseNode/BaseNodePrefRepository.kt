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
package com.tari.android.wallet.data.sharedPrefs.baseNode

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseNodePrefRepository @Inject constructor(
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkPrefRepository,
) : CommonPrefRepository(networkRepository) {

    private object Key {
        const val CURRENT_BASE_NODE = "tari_wallet_current_base_node"
        const val USER_BASE_NODE_LIST = "tari_wallet_user_base_nodes"
        const val FFI_BASE_NODE_LIST = "FFI_BASE_NODE_LIST"
    }

    var currentBaseNode: BaseNodeDto? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs, commonRepository = this,
        name = formatKey(Key.CURRENT_BASE_NODE),
        type = BaseNodeDto::class.java,
    )

    var ffiBaseNodes: BaseNodeList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.FFI_BASE_NODE_LIST),
        type = BaseNodeList::class.java,
        defValue = BaseNodeList(),
    )

    var userBaseNodes: BaseNodeList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.USER_BASE_NODE_LIST),
        type = BaseNodeList::class.java,
        defValue = BaseNodeList(),
    )

    fun clear() {
        currentBaseNode = null
        userBaseNodes = BaseNodeList()
        ffiBaseNodes = BaseNodeList()
    }
}

class BaseNodeList(baseNodes: List<BaseNodeDto>) : ArrayList<BaseNodeDto>(baseNodes) {
    constructor() : this(emptyList())
}