package com.tari.android.wallet.application.baseNodes

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeList
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.TariBaseNodeState
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.io.IOUtils
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val REGEX_ONION_WITH_NAME = "(.+::[A-Fa-f0-9 ]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)"
private const val REGEX_IPV4_WITH_NAME = "(.+::[A-Fa-f0-9 ]{64}::/ip4/[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}/tcp/[0-9]{2,6})"
private const val REGEX_ONION = "([A-Fa-f0-9 ]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)"
private const val REGEX_IPV4 = "([A-Fa-f0-9 ]{64}::/ip4/[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}/tcp/[0-9]{2,6})"

@Singleton
class BaseNodesManager @Inject constructor(
    private val context: Context,
    private val baseNodeSharedRepository: BaseNodePrefRepository,
    private val networkRepository: NetworkPrefRepository,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as triples of
     * ({name}, {public_key_hex}, {public_address}).
     */
    val baseNodeList: List<BaseNodeDto>
        get() = if (DebugConfig.hardcodedBaseNodes) {
            loadBaseNodesFromResource()
        } else {
            baseNodeSharedRepository.ffiBaseNodes
        }

    private val _walletScannedHeight = MutableStateFlow(0)
    val walletScannedHeight = _walletScannedHeight.asStateFlow()

    private val _baseNodeState = MutableStateFlow(
        TariBaseNodeState(
            heightOfLongestChain = BigInteger.ZERO,
            nodeId = null,
        )
    )
    val baseNodeState = _baseNodeState.asStateFlow()

    val currentBaseNode: BaseNodeDto?
        get() = baseNodeSharedRepository.currentBaseNode

    val userBaseNodes: List<BaseNodeDto>
        get() = baseNodeSharedRepository.userBaseNodes

    /**
     * Select a base node randomly from the list of base nodes in base_nodes.tx, and sets
     * the wallet and stored the values in shared prefs.
     * @return the selected base node or null if the list is at its end.
     */
    @Synchronized
    fun setNextBaseNode(): BaseNodeDto? {
        val currentBaseNode = baseNodeSharedRepository.currentBaseNode ?: baseNodeList.firstOrNull()
        val nextBaseNode = baseNodeList.getOrNull(baseNodeList.indexOf(currentBaseNode) + 1)

        baseNodeSharedRepository.currentBaseNode = nextBaseNode

        return nextBaseNode
    }

    /**
     * Sets the base node to the given base node.
     * Need to call WalletManager.syncBaseNode() after this method.
     */
    fun setBaseNode(baseNode: BaseNodeDto) {
        baseNodeSharedRepository.currentBaseNode = baseNode
    }

    fun addUserBaseNode(baseNode: BaseNodeDto) {
        baseNodeSharedRepository.userBaseNodes.apply {
            add(baseNode)
            baseNodeSharedRepository.userBaseNodes = this
        }
    }

    fun deleteUserBaseNode(baseNode: BaseNodeDto) {
        baseNodeSharedRepository.userBaseNodes.apply {
            remove(baseNode)
            baseNodeSharedRepository.userBaseNodes = this
        }
    }

    fun saveBaseNodeState(ffiBaseNodeState: FFITariBaseNodeState) {
        _baseNodeState.update { TariBaseNodeState(ffiBaseNodeState) }
    }

    fun saveWalletScannedHeight(height: Int) {
        _walletScannedHeight.update { height }
    }

    /**
     * address should be in the format of hex::/onion3/{public_key} or hex::/ip4/{ip}/tcp/{port}
     */
    fun isValidBaseNode(address: String): Boolean {
        return Regex(REGEX_ONION).matches(address) || Regex(REGEX_IPV4).matches(address)
    }

    fun refreshBaseNodeList(wallet: FFIWallet) {
        baseNodeSharedRepository.ffiBaseNodes = loadBaseNodesFromFFI(wallet)
    }

    private fun loadBaseNodesFromFFI(wallet: FFIWallet): BaseNodeList = wallet.getBaseNodePeers()
        .mapIndexed { index, publicKey ->
            BaseNodeDto(
                name = "${networkRepository.currentNetwork.network.displayName} ${index + 1}",
                publicKeyHex = publicKey.hex,
            )
        }
        .let { BaseNodeList(it) }
        .also { list -> logger.i("baseNodeList from FFI: \n${list.joinToString(separator = "\n")}") }


    private fun loadBaseNodesFromResource(): List<BaseNodeDto> =
        IOUtils.toString(context.resources.openRawResource(getBaseNodeResource(networkRepository.currentNetwork.network)), "UTF-8")
            .let { baseNodeListContent ->
                logger.i("baseNodeList from local resource file: $baseNodeListContent")
                listOf(
                    findAndAddBaseNode(baseNodeListContent, REGEX_ONION_WITH_NAME),
                    findAndAddBaseNode(baseNodeListContent, REGEX_IPV4_WITH_NAME)
                ).flatten().sortedBy { it.name }
            }

    private fun getBaseNodeResource(network: Network): Int = when (network) {
        Network.STAGENET -> R.raw.stagenet_base_nodes
        Network.NEXTNET -> R.raw.nextnet_base_nodes
        else -> error("No base nodes for network: $network")
    }

    private fun findAndAddBaseNode(fileContent: String, regex: String): List<BaseNodeDto> =
        Regex(regex).findAll(fileContent).map { matchResult ->
            val (name, publicKeyHex, address) = matchResult.value.split("::")
            BaseNodeDto(name, publicKeyHex, address).also { logger.i(it.toString()) }
        }.toList()
}
