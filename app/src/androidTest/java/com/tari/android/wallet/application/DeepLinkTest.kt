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
package com.tari.android.wallet.application

import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkTest {

    private val networkRepository: NetworkPrefRepository = NetworkRepositoryMock()
    private val deeplinkHandler: DeeplinkHandler = DeeplinkHandler(networkRepository)
    private val currentNetwork = Network.STAGENET


    @Test
    fun assertBaseNodeName() {
        val deeplink =
            "tari://${currentNetwork.uriComponent}/${DeepLink.AddBaseNode.COMMAND_ADD_NODE}?${DeepLink.AddBaseNode.KEY_NAME}=base_node_test"
        val result = deeplinkHandler.handle(deeplink) as? DeepLink.AddBaseNode
        assertEquals(result!!.name, "base_node_test")
    }

    @Test
    fun assertBaseNodePeer() {
        val deeplink = "tari://${currentNetwork.uriComponent}/${DeepLink.AddBaseNode.COMMAND_ADD_NODE}?${DeepLink.AddBaseNode.KEY_PEER}=$PEER"
        val result = deeplinkHandler.handle(deeplink) as? DeepLink.AddBaseNode
        assertEquals(result!!.peer, PEER)
    }

    companion object {
        private const val PUBLIC_KEY = "2e93c460DF49D8CFBBF7A06DD9004C25A84F92584F7D0AC5E30BD8E0BEEE9A43"
        private const val PEER =
            "3e0321c0928ca559ab3c0a396272dfaea705efce88440611a38ff3898b097217::/onion3/sl5ledjoaisst6d4fh7kde746dwweuge4m4mf5nkzdhmy57uwgtb7qqd:18141"
    }

    class NetworkRepositoryMock : NetworkPrefRepository {
        private val network: Network = Network.STAGENET
        private val tariNetwork = TariNetwork(
            network = network,
            dnsPeer = "seeds.stagenet.tari.com",
            ticker = "xtr",
            blockExplorerUrl = null,
            recommended = true,
        )

        override val defaultNetwork: TariNetwork = tariNetwork
        override var supportedNetworks: List<TariNetwork> = listOf(tariNetwork)
        override var currentNetwork: TariNetwork = TariNetwork(
            network = network,
            dnsPeer = "seeds.stagenet.tari.com",
            ticker = "",
            blockExplorerUrl = null,
            recommended = true,
        )
        override var ffiNetwork: Network? = network
    }
}
