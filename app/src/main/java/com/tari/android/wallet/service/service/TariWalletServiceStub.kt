package com.tari.android.wallet.service.service

import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetFaucetRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetUtxoList
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.TariWalletServiceListener
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.service.faucet.TestnetFaucetService
import com.tari.android.wallet.service.faucet.TestnetTariRequestException
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.util.Constants
import java.math.BigInteger
import java.util.*

class TariWalletServiceStub(
    private val wallet: FFIWallet,
    private val testnetFaucetRepository: TestnetFaucetRepository,
    private val testnetFaucetService: TestnetFaucetService,
    private val baseNodeSharedPrefsRepository: BaseNodeSharedRepository,
    private val resourceManager: ResourceManager,
    private val walletServiceListener: FFIWalletListenerImpl
) : TariWalletService.Stub() {

    private val logger
        get() = Logger.t(WalletService::class.simpleName)

    private var _cachedContacts: List<Contact>? = null
    private val cachedContacts: List<Contact>
        @Synchronized get() {
            _cachedContacts?.let { return it }
            val contactsFFI = wallet.getContacts()
            val contacts = mutableListOf<Contact>()
            for (i in 0 until contactsFFI.getLength()) {
                val contactFFI = contactsFFI.getAt(i)
                val publicKeyFFI = contactFFI.getPublicKey()
                contacts.add(
                    Contact(
                        publicKeyFromFFI(publicKeyFFI),
                        contactFFI.getAlias()
                    )
                )
                // destroy native objects
                publicKeyFFI.destroy()
                contactFFI.destroy()
            }
            // destroy native collection
            contactsFFI.destroy()
            return contacts.sortedWith(compareBy { it.alias }).also { _cachedContacts = it }
        }


    private fun mapThrowableIntoError(walletError: WalletError, throwable: Throwable) {
        if (throwable is FFIException) {
            if (throwable.error != null) {
                walletError.code = throwable.error.code
                return
            }
        }
        walletError.code = WalletError.UnknownError.code
    }

    private fun <T> runMapping(walletError: WalletError, onError: (Throwable) -> (Unit) = {}, action: () -> T?): T? {
        return try {
            action()
        } catch (throwable: Throwable) {
            onError(throwable)
            mapThrowableIntoError(walletError, throwable)
            null
        }
    }

    private fun getContactByPublicKeyHexString(hexString: String): Contact? = cachedContacts.firstOrNull { it.publicKey.hexString == hexString }

    override fun registerListener(listener: TariWalletServiceListener): Boolean {
        walletServiceListener.listeners.add(listener)
        listener.asBinder().linkToDeath({ walletServiceListener.listeners.remove(listener) }, 0)
        return true
    }

    override fun unregisterListener(listener: TariWalletServiceListener): Boolean = walletServiceListener.listeners.remove(listener)

    override fun getPublicKeyHexString(error: WalletError): String? = runMapping(error) { wallet.getPublicKey().toString() }

    override fun getBalanceInfo(error: WalletError): BalanceInfo? = runMapping(error) { wallet.getBalance() }

    override fun estimateTxFee(amount: MicroTari, error: WalletError, feePerGram: MicroTari?): MicroTari? = runMapping(error) {
        val defaultKernelCount = BigInteger("1")
        val defaultOutputCount = BigInteger("2")
        val gram = feePerGram?.value ?: Constants.Wallet.defaultFeePerGram.value
        MicroTari(wallet.estimateTxFee(amount.value, gram, defaultKernelCount, defaultOutputCount))
    }

    /**
     * Get all contacts.
     */
    override fun getContacts(error: WalletError): List<Contact>? = runMapping(error) { cachedContacts }

    /**
     * Get all completed transactions.
     * Client-facing function.
     */
    override fun getCompletedTxs(error: WalletError): List<CompletedTx>? = runMapping(error) {
        val completedTxsFFI = wallet.getCompletedTxs()
        (0 until completedTxsFFI.getLength())
            .map { CompletedTx(completedTxsFFI.getAt(it)) }
            .also { completedTxsFFI.destroy() }
    }

    /**
     * Get all cancelledTxs transactions.
     * Client-facing function.
     */
    override fun getCancelledTxs(error: WalletError): List<CancelledTx>? = runMapping(error) {
        val canceledTxsFFI = wallet.getCancelledTxs()
        (0 until canceledTxsFFI.getLength())
            .map { CancelledTx(canceledTxsFFI.getAt(it)) }
            .also { canceledTxsFFI.destroy() }
    }

    /**
     * Get completed transaction by id.
     * Client-facing function.
     */
    override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? = runMapping(error) {
        CancelledTx(wallet.getCancelledTxById(id.value))
    }

    /**
     * Get completed transaction by id.
     * Client-facing function.
     */
    override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? = runMapping(error) {
        CompletedTx(wallet.getCompletedTxById(id.value))
    }

    /**
     * Get all pending inbound transactions.
     * Client-facing function.
     */
    override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? = runMapping(error) {
        val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
        (0 until pendingInboundTxsFFI.getLength())
            .map { PendingInboundTx(pendingInboundTxsFFI.getAt(it)) }
            .also { pendingInboundTxsFFI.destroy() }
    }

    /**
     * Get pending inbound transaction by id.
     * Client-facing function.
     */
    override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? = runMapping(error) {
        PendingInboundTx(wallet.getPendingInboundTxById(id.value))
    }

    /**
     * Get all pending outbound transactions.
     * Client-facing function.
     */
    override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? = runMapping(error) {
        val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
        (0 until pendingOutboundTxsFFI.getLength())
            .map { PendingOutboundTx(pendingOutboundTxsFFI.getAt(it)) }
            .also { pendingOutboundTxsFFI.destroy() }
    }

    /**
     * Get pending outbound transaction by id.
     * Client-facing function.
     */
    override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? = runMapping(error) {
        PendingOutboundTx(wallet.getPendingOutboundTxById(id.value))
    }

    override fun cancelPendingTx(id: TxId, error: WalletError): Boolean = runMapping(error) { wallet.cancelPendingTx(id.value) } ?: false

    override fun addBaseNodePeer(baseNodePublicKey: String, baseNodeAddress: String, error: WalletError): Boolean = runMapping(error) {
        val publicKeyFFI = FFIPublicKey(HexString(baseNodePublicKey))
        val result = wallet.addBaseNodePeer(publicKeyFFI, baseNodeAddress)
        publicKeyFFI.destroy()
        if (result) {
            walletServiceListener.baseNodeValidationStatusMap.clear()
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.NotStarted)
        }
        result
    } ?: false

    override fun startBaseNodeSync(error: WalletError): Boolean = runMapping(error, {
        logger.e(it, "Base node sync failed")
        baseNodeSharedPrefsRepository.baseNodeLastSyncResult = false
        walletServiceListener.baseNodeValidationStatusMap.clear()
        EventBus.baseNodeSyncState.post(BaseNodeSyncState.Failed)
    }) {
        walletServiceListener.baseNodeValidationStatusMap.clear()
        walletServiceListener.baseNodeValidationStatusMap[BaseNodeValidationType.TXO] = Pair(wallet.startTXOValidation(), null)
        walletServiceListener.baseNodeValidationStatusMap[BaseNodeValidationType.TX] = Pair(wallet.startTxValidation(), null)
        baseNodeSharedPrefsRepository.baseNodeLastSyncResult = null
        true
    } ?: false

    override fun sendTari(
        user: User,
        amount: MicroTari,
        feePerGram: MicroTari,
        message: String,
        isOneSidePayment: Boolean,
        error: WalletError
    ): TxId? = runMapping(error) {
        val recipientPublicKeyHex = user.publicKey.hexString
        val publicKeyFFI = FFIPublicKey(HexString(recipientPublicKeyHex))
        val txId = wallet.sendTx(publicKeyFFI, amount.value, feePerGram.value, message, isOneSidePayment)
        publicKeyFFI.destroy()
        walletServiceListener.outboundTxIdsToBePushNotified.add(Pair(txId, recipientPublicKeyHex.lowercase(Locale.ENGLISH)))
        TxId(txId)
    }

    // region FFI to model extraction functions
    private fun publicKeyFromFFI(publicKeyFFI: FFIPublicKey): PublicKey {
        return PublicKey(publicKeyFFI.toString(), publicKeyFFI.getEmojiId())
    }

    override fun requestTestnetTari(error: WalletError) {
        // avoid multiple faucet requests
        if (testnetFaucetRepository.faucetTestnetTariRequestCompleted) return
        // get public key
        val publicKeyHexString = getPublicKeyHexString(error)
        if (error.code != WalletError.NoError.code || publicKeyHexString == null) {
            notifyTestnetTariRequestFailed("Service error.")
            return
        }

        val message = "$MESSAGE_PREFIX $publicKeyHexString"
        val signing = wallet.signMessage(message).split("|")
        val signature = signing[0]
        val nonce = signing[1]

        testnetFaucetService.requestMaxTestnetTari(
            publicKeyHexString,
            signature,
            nonce,
            { result ->
                val senderPublicKeyFFI = FFIPublicKey(HexString(result.walletId))
                // add contact
                FFIContact("TariBot", senderPublicKeyFFI).also {
                    wallet.addUpdateContact(it)
                    it.destroy()
                }
                senderPublicKeyFFI.destroy()
                // update the keys with sender public key hex
                result.keys.forEach { key -> key.senderPublicKeyHex = result.walletId }
                // store the UTXO keys
                testnetFaucetRepository.testnetTariUTXOKeyList = TestnetUtxoList(result.keys)

                // post event to bus for the listeners
                EventBus.post(Event.Testnet.TestnetTariRequestSuccessful())
                // notify external listeners
                walletServiceListener.listeners.iterator().forEach { it.onTestnetTariRequestSuccess() }
            },
            {
                val errorMessage = resourceManager.getString(R.string.wallet_service_error_testnet_tari_request) + " " + it.message
                logger.e(errorMessage + "failed on requesting faucet")
                if (it is TestnetTariRequestException) {
                    notifyTestnetTariRequestFailed(errorMessage)
                } else {
                    notifyTestnetTariRequestFailed(resourceManager.getString((R.string.wallet_service_error_no_internet_connection)))
                }
            }
        )
    }

    override fun importTestnetUTXO(txMessage: String, error: WalletError): CompletedTx? {
        val keys = testnetFaucetRepository.testnetTariUTXOKeyList.orEmpty().toMutableList()
        if (keys.isEmpty()) return null

        return runCatching {
            val firstUTXOKey = keys.first()
            val senderPublicKeyFFI = FFIPublicKey(HexString(firstUTXOKey.senderPublicKeyHex!!))
            val privateKey = FFIPrivateKey(HexString(firstUTXOKey.key))
            val scriptPrivateKey = FFIPrivateKey(HexString(firstUTXOKey.key))
            val amount = BigInteger(firstUTXOKey.value)
            val senderPublicKey = FFIPublicKey(HexString(firstUTXOKey.output.senderOffsetPublicKey))
            val signature = FFITariCommitmentSignature(
                FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.public_nonce)),
                FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.u)),
                FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.v))
            )
            val covenant = FFICovenant(FFIByteVector(HexString(firstUTXOKey.output.covenant)))
            val outputFeatures = FFIOutputFeatures('0', 0, FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.public_nonce)))
            val txId = wallet.importUTXO(
                amount,
                txMessage,
                privateKey,
                senderPublicKeyFFI,
                outputFeatures,
                signature,
                covenant,
                senderPublicKey,
                scriptPrivateKey
            )
            privateKey.destroy()
            senderPublicKeyFFI.destroy()
            signature.destroy()
            // remove the used key
            keys.remove(firstUTXOKey)
            testnetFaucetRepository.testnetTariUTXOKeyList = TestnetUtxoList(keys)
            // get transaction and post notification
            val tx = getCompletedTxById(TxId(txId), error)
            if (error != WalletError.NoError || tx == null) return null

            walletServiceListener.postTxNotification(tx)
            tx
        }.getOrNull()
    }

    override fun removeContact(contact: Contact, error: WalletError): Boolean = runMapping(error) {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val publicKeyFFI = contactFFI.getPublicKey()
            if (publicKeyFFI.toString() == contact.publicKey.hexString) {
                return@runMapping wallet.removeContact(contactFFI).also {
                    publicKeyFFI.destroy()
                    contactFFI.destroy()
                    contactsFFI.destroy()
                    _cachedContacts = null
                }
            }
            publicKeyFFI.destroy()
            contactFFI.destroy()
        }
        contactsFFI.destroy()
        false
    } ?: false

    private fun notifyTestnetTariRequestFailed(error: String) {
        // post event to bus for the listeners
        EventBus.post(Event.Testnet.TestnetTariRequestError(error))
        // notify external listeners
        walletServiceListener.listeners.iterator().forEach { listener -> listener.onTestnetTariRequestError(error) }
    }

    override fun updateContactAlias(publicKey: PublicKey, alias: String, error: WalletError): Boolean = runMapping(error) {
        val publicKeyFFI = FFIPublicKey(HexString(publicKey.hexString))
        val contact = FFIContact(alias, publicKeyFFI)
        wallet.addUpdateContact(contact).also {
            publicKeyFFI.destroy()
            contact.destroy()
            _cachedContacts = null
        }
    } ?: false

    /**
     * @return public key constructed from input emoji id. Null if the emoji id is invalid
     * or it does not correspond to a public key.
     */
    override fun getPublicKeyFromEmojiId(emojiId: String?): PublicKey? =
        runCatching { FFIPublicKey(emojiId.orEmpty()).run { publicKeyFromFFI(this).also { destroy() } } }.getOrNull()

    /**
     * @return public key constructed from input public key hex string id. Null if the emoji id
     * is invalid or it does not correspond to a public key.
     */
    override fun getPublicKeyFromHexString(publicKeyHex: String?): PublicKey? = runCatching {
        FFIPublicKey(HexString(publicKeyHex ?: "")).run { publicKeyFromFFI(this).also { destroy() } }
    }.getOrNull()

    override fun setKeyValue(key: String, value: String, error: WalletError): Boolean = runMapping(error) { wallet.setKeyValue(key, value) } ?: false

    override fun getKeyValue(key: String, error: WalletError): String? = runMapping(error) { wallet.getKeyValue(key) }

    override fun removeKeyValue(key: String, error: WalletError): Boolean = runMapping(error) { wallet.removeKeyValue(key) } ?: false

    override fun getRequiredConfirmationCount(error: WalletError): Long = runMapping(error) { wallet.getRequiredConfirmationCount().toLong() } ?: 0

    override fun setRequiredConfirmationCount(number: Long, error: WalletError) {
        runMapping(error) { wallet.setRequiredConfirmationCount(BigInteger.valueOf(number)) }
    }

    override fun getSeedWords(error: WalletError): List<String>? = runMapping(error) {
        val seedWordsFFI = wallet.getSeedWords()
        (0 until seedWordsFFI.getLength())
            .map { seedWordsFFI.getAt(it) }
            .also { seedWordsFFI.destroy() }
    }

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? =
        runMapping(error) { wallet.getUtxos(page, pageSize, sorting) }

    override fun getAllUtxos(error: WalletError): TariVector? =
        runMapping(error) { wallet.getAllUtxos() }

    override fun joinUtxos(utxos: List<TariUtxo>, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.joinUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.splitUtxos(utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun previewJoinUtxos(utxos: List<TariUtxo>, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.joinPreviewUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
        result
    }

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError): TariCoinPreview? =
        runMapping(walletError) {
            val ffiError = FFIError()
            val result = wallet.splitPreviewUtxos(
                utxos.map { it.commitment }.toTypedArray(),
                splitCount,
                Constants.Wallet.defaultFeePerGram.value,
                ffiError
            )
            walletError.code = ffiError.code
            result
        }


    companion object {
        private const val MESSAGE_PREFIX = "Hello Tari from"
    }
}