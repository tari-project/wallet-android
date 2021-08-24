package com.tari.android.wallet.application.baseNodes

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import org.apache.commons.io.IOUtils

class BaseNodes(
    private val context: Context,
    private val sharedPrefsRepository: SharedPrefsRepository
) {

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
        sharedPrefsRepository.baseNodeLastSyncResult = null
        sharedPrefsRepository.baseNodeName = baseNode.name
        sharedPrefsRepository.baseNodePublicKeyHex = baseNode.publicKeyHex
        sharedPrefsRepository.baseNodeAddress = baseNode.address
        sharedPrefsRepository.baseNodeIsUserCustom = false

        val baseNodeKeyFFI = FFIPublicKey(HexString(baseNode.publicKeyHex))
        FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNode.address)
        baseNodeKeyFFI.destroy()
    }
}

