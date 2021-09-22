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
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository.Key.baseNodeLastSyncResultField
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository.Key.currentBaseNodeField
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository.Key.userBaseNodeListField
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.model.BaseNodeValidationResult

class BaseNodeSharedRepository(sharedPrefs: SharedPreferences) {

    private object Key {
        const val currentBaseNodeField = "tari_wallet_current_base_node"
        const val userBaseNodeListField = "tari_wallet_user_base_nodes"

        const val baseNodeLastSyncResultField = "tari_wallet_base_node_last_sync_result"
    }

    var currentBaseNode: BaseNodeDto? by SharedPrefGsonDelegate(sharedPrefs, currentBaseNodeField, BaseNodeDto::class.java)

    var userBaseNodes: BaseNodeList? by SharedPrefGsonDelegate(sharedPrefs, userBaseNodeListField, BaseNodeList::class.java)

    var baseNodeLastSyncResult: BaseNodeValidationResult? by SharedPrefGsonDelegate(
        sharedPrefs, baseNodeLastSyncResultField, BaseNodeValidationResult::class.java
    )


    fun deleteUserBaseNode(baseNodeDto: BaseNodeDto) {
        userBaseNodes.orEmpty().apply {
            remove(baseNodeDto)
            userBaseNodes = this
        }
    }

    fun addUserBaseNode(baseNodeDto: BaseNodeDto) {
        userBaseNodes.orEmpty().apply {
            add(baseNodeDto)
            userBaseNodes = this
        }
    }

    fun clear() {
        baseNodeLastSyncResult = null
        currentBaseNode = null
        userBaseNodes = null
    }
}