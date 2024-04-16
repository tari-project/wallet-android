package com.tari.android.wallet.application.baseNodes

import android.content.Context
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.service.connection.ServiceConnectionStatus
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import io.reactivex.disposables.CompositeDisposable
import org.apache.commons.io.IOUtils

private const val REGEX_ONION = "(.+::[A-Za-z0-9 ]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)"
private const val REGEX_IPV4 = "(.+::[A-Za-z0-9 ]{64}::/ip4/[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}/tcp/[0-9]{2,6})"

class BaseNodes(
    private val context: Context,
    private val baseNodeSharedRepository: BaseNodeSharedRepository,
    private val networkRepository: NetworkRepository,
) {
    private val logger = Logger.t(this::class.simpleName)

    private val compositeDisposable = CompositeDisposable()

    private val serviceConnection = TariWalletServiceConnection()
    private val walletService
        get() = serviceConnection.currentState.service!!

    private lateinit var baseNodeIterator: Iterator<BaseNodeDto>

    init {
        serviceConnection.connection.subscribe {
            if (it.status == ServiceConnectionStatus.CONNECTED) {
                startSync()
            }
        }.addTo(compositeDisposable)

        EventBus.walletState.publishSubject.subscribe {
            startSync()
        }.addTo(compositeDisposable)
    }

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as pairs of
     * ({name}, {public_key_hex}, {public_address}).
     */
    val baseNodeList by lazy {
        val fileContent = IOUtils.toString(
            context.resources.openRawResource(getBaseNodeResource(networkRepository.currentNetwork!!.network)),
            "UTF-8"
        )
        logger.i("baseNodeList: $fileContent")
        val list = mutableListOf<BaseNodeDto>()
        val onionBaseNodes = findAndAddBaseNode(fileContent, REGEX_ONION).toList()
        val ipV4BaseNodes = findAndAddBaseNode(fileContent, REGEX_IPV4).toList()
        list.addAll(onionBaseNodes)
        list.addAll(ipV4BaseNodes)
        list.sortedBy { it.name }
    }

    private fun findAndAddBaseNode(fileContent: String, regex: String): Sequence<BaseNodeDto> {
        return Regex(regex).findAll(fileContent).map { matchResult ->
            val (name, publicKeyHex, address) = matchResult.value.split("::")
            logger.i("baseNodeList0: $name, baseNodeList1: $publicKeyHex, baseNodeList2: $address")
            BaseNodeDto(name, publicKeyHex, address)
        }
    }

    /**
     * Select a base node randomly from the list of base nodes in base_nodes.tx, and sets
     * the wallet and stored the values in shared prefs.
     */
    @Synchronized
    fun setNextBaseNode() {
        if (!this::baseNodeIterator.isInitialized || !baseNodeIterator.hasNext()) {
            baseNodeIterator = baseNodeList.iterator()
        }
        val baseNode = baseNodeIterator.next()
        setBaseNode(baseNode)
    }

    fun setBaseNode(baseNode: BaseNodeDto) {
        baseNodeSharedRepository.baseNodeLastSyncResult = null
        baseNodeSharedRepository.currentBaseNode = baseNode
        startSync()
    }

    fun startSync() {
        try {
            logger.i("startSync")
            //essential for wallet creation flow
            val baseNode = baseNodeSharedRepository.currentBaseNode ?: return
            serviceConnection.currentState.service ?: return
            if (EventBus.walletState.publishSubject.value != WalletState.Running) return

            logger.i("startSync:publicKeyHex: ${baseNode.publicKeyHex}")
            logger.i("startSync:address: ${baseNode.address}")
            logger.i("startSync:userBaseNodes: ${Gson().toJson(baseNodeSharedRepository.userBaseNodes)}")
            val baseNodeKeyFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
            FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNode.address)
            baseNodeKeyFFI.destroy()
            walletService.getWithError { error, wallet -> wallet.startBaseNodeSync(error) }
        } catch (e: Throwable) {
            logger.i("startSync:error connecting to base node: ${e.message}")
            setNextBaseNode()
            startSync()
        }
    }

    private fun getBaseNodeResource(network: Network): Int = when (network) {
        Network.STAGENET -> R.raw.stagenet_base_nodes
        Network.NEXTNET -> R.raw.nextnet_base_nodes
        else -> error("No base nodes for network: $network")
    }
}
