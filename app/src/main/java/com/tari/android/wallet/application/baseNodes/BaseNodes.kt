package com.tari.android.wallet.application.baseNodes

import android.content.Context
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

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as pairs of
     * ({name}, {public_key_hex}, {public_address}).
     */
    val baseNodeList by lazy {
        val fileContent = IOUtils.toString(
            context.resources.openRawResource(getBaseNodeResource(networkRepository.currentNetwork!!.network)),
            "UTF-8"
        )
        Regex("(.+::[A-Za-z0-9]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)").findAll(fileContent).map { matchResult ->
            val tripleString = matchResult.value.split("::")
            BaseNodeDto(tripleString[0], tripleString[1], tripleString[2])
        }
    }

    /**
     * Select a base node randomly from the list of base nodes in base_nodes.tx, and sets
     * the wallet and stored the values in shared prefs.
     */
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
        //essential for wallet creation flow
        val baseNode = baseNodeSharedRepository.currentBaseNode ?: return
        serviceConnection.currentState.service ?: return
        if (EventBus.walletState.publishSubject.value != WalletState.Running) return

        val baseNodeKeyFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
        FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNode.address)
        baseNodeKeyFFI.destroy()
        walletService.getWithError { error, wallet -> wallet.startBaseNodeSync(error) }
    }

    @Suppress("UNUSED_EXPRESSION")
    private fun getBaseNodeResource(network: Network): Int = when(network) {
        else -> R.raw.esmeralda_base_nodes
    }
}
