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

class BaseNodes(
    private val context: Context,
    private val baseNodeSharedRepository: BaseNodeSharedRepository,
    private val networkRepository: NetworkRepository,
) {
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

    val onionRegex = "(.+::[A-Za-z0-9 ]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)"
    val ipV4Regex = "(.+::[A-Za-z0-9 ]{64}::/ip4/[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}/tcp/[0-9]{2,6})"

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as pairs of
     * ({name}, {public_key_hex}, {public_address}).
     */
    val baseNodeList by lazy {
        val fileContent = IOUtils.toString(
            context.resources.openRawResource(getBaseNodeResource(networkRepository.currentNetwork!!.network)),
            "UTF-8"
        )
        Logger.t(this::class.simpleName).e("baseNodeList: $fileContent")
        val list = mutableListOf<BaseNodeDto>()
        val onionBaseNodes = findAndAddBaseNode(fileContent, onionRegex).toList()
        val ipV4BaseNodes = findAndAddBaseNode(fileContent, ipV4Regex).toList()
        list.addAll(onionBaseNodes)
        list.addAll(ipV4BaseNodes)
        list.sortedBy { it.name }
    }

    private fun findAndAddBaseNode(fileContent: String, regex: String): Sequence<BaseNodeDto> {
        return Regex(regex).findAll(fileContent).map { matchResult ->
            val tripleString = matchResult.value.split("::")
            Logger.t(this::class.simpleName).i("baseNodeList0: $tripleString, baseNodeList1: ${tripleString[1]}, baseNodeList2: ${tripleString[2]}")
            BaseNodeDto(tripleString[0], tripleString[1], tripleString[2])
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
            Logger.t(this::class.simpleName).i("startSync")
            //essential for wallet creation flow
            val baseNode = baseNodeSharedRepository.currentBaseNode ?: return
            serviceConnection.currentState.service ?: return
            if (EventBus.walletState.publishSubject.value != WalletState.Running) return

            Logger.t(this::class.simpleName).i("startSync:publicKeyHex: ${baseNode.publicKeyHex}")
            Logger.t(this::class.simpleName).i("startSync:address: ${baseNode.address}")
            Logger.t(this::class.simpleName).i("startSync:address: ${Gson().toJson(baseNodeSharedRepository.userBaseNodes)}")
            val baseNodeKeyFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
            FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNode.address)
            baseNodeKeyFFI.destroy()
            walletService.getWithError { error, wallet -> wallet.startBaseNodeSync(error) }
        } catch (e: Throwable) {
            Logger.t(this::class.simpleName).e("startSync")
            setNextBaseNode()
            startSync()
        }
    }
    @Suppress("UNUSED_EXPRESSION")
    private fun getBaseNodeResource(network: Network): Int = when (network) {
        else -> R.raw.stagenet_base_nodes
    }
}
