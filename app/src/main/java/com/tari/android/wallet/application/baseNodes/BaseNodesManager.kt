package com.tari.android.wallet.application.baseNodes

import android.content.Context
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.application.walletManager.WalletStateHandler
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeList
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val serviceConnection: TariWalletServiceConnection,
    private val walletStateHandler: WalletStateHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
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

    val networkBlockHeight: BigInteger
        get() = baseNodeSharedRepository.baseNodeHeightOfLongestChain

    /**
     * Select a base node randomly from the list of base nodes in base_nodes.tx, and sets
     * the wallet and stored the values in shared prefs.
     * @return the selected base node or null if the list is at its end.
     */
    @Synchronized
    fun setNextBaseNode(): BaseNodeDto? {
        val currentBaseNode = baseNodeSharedRepository.currentBaseNode ?: baseNodeList.firstOrNull()
        val nextBaseNode = baseNodeList.getOrNull(baseNodeList.indexOf(currentBaseNode) + 1)

        baseNodeSharedRepository.baseNodeLastSyncResult = null
        baseNodeSharedRepository.currentBaseNode = nextBaseNode

        return nextBaseNode
    }

    fun setBaseNode(baseNode: BaseNodeDto) {
        baseNodeSharedRepository.baseNodeLastSyncResult = null
        baseNodeSharedRepository.currentBaseNode = baseNode
        startSync()
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

    fun saveBaseNodeState(baseNodeState: FFITariBaseNodeState) {
        baseNodeSharedRepository.baseNodeHeightOfLongestChain = baseNodeState.getHeightOfLongestChain()
    }

    fun startSync() {
        //essential for wallet creation flow
        var currentBaseNode: BaseNodeDto? = baseNodeSharedRepository.currentBaseNode ?: return

        applicationScope.launch(Dispatchers.IO) {
            serviceConnection.doOnWalletServiceConnected { walletService ->
                walletStateHandler.doOnWalletRunning { ffiWallet ->
                    while (currentBaseNode != null) {
                        try {
                            currentBaseNode?.let {
                                logger.i("startSync")
                                logger.i("startSync:publicKeyHex: ${it.publicKeyHex}")
                                logger.i("startSync:address: ${it.address}")
                                logger.i("startSync:userBaseNodes: ${Gson().toJson(baseNodeSharedRepository.userBaseNodes)}")
                                val baseNodeKeyFFI = FFIPublicKey(HexString(it.publicKeyHex))
                                ffiWallet.addBaseNodePeer(baseNodeKeyFFI, it.address)
                                baseNodeKeyFFI.destroy()
                                walletService.getWithError { error, wallet -> wallet.startBaseNodeSync(error) }
                            }
                            break
                        } catch (e: Throwable) {
                            logger.i("startSync:error connecting to base node $currentBaseNode with an error: ${e.message}")
                            currentBaseNode = setNextBaseNode()
                        }
                    }

                    if (currentBaseNode == null) {
                        logger.e("startSync: cannot connect to any base node")
                    }
                }
            }
        }
    }

    /**
     * address should be in the format of hex::/onion3/{public_key} or hex::/ip4/{ip}/tcp/{port}
     */
    fun isValidBaseNode(address: String): Boolean {
        return Regex(REGEX_ONION).matches(address) || Regex(REGEX_IPV4).matches(address)
    }

    fun refreshBaseNodeList() {
        baseNodeSharedRepository.ffiBaseNodes = loadBaseNodesFromFFI()
    }

    private fun loadBaseNodesFromFFI(): BaseNodeList = FFIWallet.instance?.getBaseNodePeers()
        ?.mapIndexed { index, publicKey ->
            BaseNodeDto(
                name = "${networkRepository.currentNetwork.network.displayName} ${index + 1}",
                publicKeyHex = publicKey.hex,
            )
        }?.toMutableList().orEmpty()
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
