package com.tari.android.wallet.application.baseNodes

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.extension.executeWithError
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import org.apache.commons.io.IOUtils

class BaseNodes(
    private val context: Context,
    private val baseNodeSharedRepository: BaseNodeSharedRepository
) {

    private val serviceConnection = TariWalletServiceConnection()
    private val walletService
        get() = serviceConnection.currentState.service!!

    private lateinit var baseNodeIterator: Iterator<BaseNodeDto>

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as pairs of
     * ({name}, {public_key_hex}, {public_address}).
     */
    val baseNodeList by lazy {
        val fileContent = IOUtils.toString(
            context.resources.openRawResource(R.raw.base_nodes),
            "UTF-8"
        )
        val baseNodes = mutableListOf<BaseNodeDto>()
        val regex = Regex("(.+::[A-Za-z0-9]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)")
        regex.findAll(fileContent).forEach { matchResult ->
            val tripleString = matchResult.value.split("::")
            baseNodes.add(
                BaseNodeDto(
                    tripleString[0],
                    tripleString[1],
                    tripleString[2]
                )
            )
        }
        baseNodes.shuffle()
        baseNodes
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
        addIntoWallet(baseNode)
    }

    fun addIntoWallet(baseNode: BaseNodeDto) {
        val baseNodeKeyFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
        FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNode.address)
        baseNodeKeyFFI.destroy()
        walletService.executeWithError { error, wallet -> wallet.startBaseNodeSync(error) }
    }
}
