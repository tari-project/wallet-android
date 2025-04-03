package com.tari.android.wallet.application

import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.ui.screen.send.finalize.FinalizeSendTxViewModel
import com.tari.android.wallet.ui.screen.send.finalize.YatFinalizeSendTxActivity
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.lib.YatConfiguration
import yat.android.lib.YatIntegration
import yat.android.lib.YatLibApi
import yat.android.sdk.models.PaymentAddressResponseResult
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionData
import java.lang.reflect.Field
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YatAdapter @Inject constructor(
    private val yatSharedRepository: YatPrefRepository,
    private val networkRepository: NetworkPrefRepository,
    private val commonRepository: CorePrefRepository
) : YatIntegration.Delegate {

    private val logger
        get() = Logger.t(YatAdapter::class.simpleName)

    val connectedYat: EmojiId?
        get() = yatSharedRepository.connectedYat

    fun initYat(application: Application) {
        val config = YatConfiguration(
            appReturnLink = BuildConfig.YAT_ORGANIZATION_RETURN_URL,
            organizationName = BuildConfig.YAT_ORGANIZATION_NAME,
            organizationKey = BuildConfig.YAT_ORGANIZATION_KEY,
        )
        YatIntegration.setup(
            context = application,
            config = config,
            colorMode = YatIntegration.ColorMode.LIGHT,
            delegate = this,
            environment = DebugConfig.yatEnvironment,
        )
    }

    /**
     * Search for Tari address by Yat emojiId
     */
    suspend fun searchTariYat(yatEmojiId: EmojiId): PaymentAddressResponseResult? = withContext(Dispatchers.IO) {
        return@withContext try {
            val tariTag = YatRecordType.XTM_ADDRESS.serializedName
            // We take only the first result because we are looking for a Tari address only
            val result = YatLibApi.emojiIDApi.lookupEmojiIDPayment(yatEmojiId, tariTag).result?.entries?.firstOrNull()?.value
            if (result == null) logger.d("Can't find Tari address for $yatEmojiId")
            result
        } catch (e: Exception) {
            logger.e("Error while searching for Tari address for $yatEmojiId:\n${e.message}")
            null
        }
    }

    /**
     * Search for all payment addresses connected to the Yat emojiId
     */
    suspend fun searchAllYats(yatEmojiId: EmojiId): Map<String, PaymentAddressResponseResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = YatLibApi.emojiIDApi.lookupEmojiIDPayment(yatEmojiId, null).result
            if (result.isNullOrEmpty()) logger.d("Can't find any Yats for $yatEmojiId")
            result.orEmpty()
        } catch (e: Exception) {
            logger.e("Error while searching for Yats for $yatEmojiId:\n${e.message}")
            emptyMap()
        }
    }

    /**
     * Check if the stored Yat emojiId isn't empty, but it is disconnected with user's wallet
     */
    suspend fun checkYatDisconnected(): Boolean = connectedYat.takeIf { it.isNullOrBlank() }
        ?.let { searchTariYat(it) }?.address
        ?.let { address -> address.lowercase() != commonRepository.walletAddressBase58.orEmpty().lowercase() } == true

    /**
     * Load all connected wallets by Yat emojiId and return them as a list of [ConnectedWallet]
     */
    suspend fun loadConnectedWallets(yatEmojiId: EmojiId): List<ConnectedWallet> = withContext(Dispatchers.IO) {
        searchAllYats(yatEmojiId)
            .map { ConnectedWallet(it.key, it.value) }
            .filter { it.name != null } // Filter out wallets with unsupported types
    }

    fun openOnboarding(context: Context) {
        val address = commonRepository.walletAddressBase58.orEmpty()
        YatIntegration.showOnboarding(context, listOf(YatRecord(YatRecordType.XTM_ADDRESS, data = address)))
    }

    fun showOutcomingFinalizeActivity(activity: Activity, transactionData: TransactionData) {
        val yatUser = transactionData.recipientContact.yat ?: return
        val currentTicker = networkRepository.currentNetwork.ticker
        val data = YatLibOutcomingTransactionData(
            amount = transactionData.amount.tariValue.toDouble(),
            currency = currentTicker,
            yat = yatUser,
        )

        val intent = Intent(activity, YatFinalizeSendTxActivity::class.java)
        intent.putExtra(YatLibOutcomingTransactionActivity.DATA_KEY, data)
        intent.putExtra(FinalizeSendTxViewModel.KEY_TRANSACTION_DATA, transactionData)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    override fun onYatIntegrationComplete(yat: String) {
        logger.d("Yat integration completed with $yat")
        yatSharedRepository.saveYat(yat)
    }

    override fun onYatIntegrationFailed(failureType: YatIntegration.FailureType) {
        logger.d("Yat integration failed $failureType")
    }

    @Parcelize
    data class ConnectedWallet(
        val key: String,
        val value: PaymentAddressResponseResult,
    ) : Parcelable {

        private val yatRecordType: YatRecordType?
            get() = names[key]

        val name: Int?
            get() {
                return when (yatRecordType) {
                    YatRecordType.BTC_ADDRESS -> R.string.contact_book_details_connected_wallets_bitcoin
                    YatRecordType.ETH_ADDRESS -> R.string.contact_book_details_connected_wallets_etherium
                    YatRecordType.XMR_STANDARD_ADDRESS -> R.string.contact_book_details_connected_wallets_monero
                    else -> null
                }
            }

        fun getExternalLink(): String = when (yatRecordType) {
            YatRecordType.BTC_ADDRESS -> "bitcoin:${value.address}"
            YatRecordType.ETH_ADDRESS -> {
                if (!value.address.startsWith("0x")) {
                    "ethereum:pay-0x${value.address}"
                } else {
                    "ethereum:pay-${value.address}"
                }
            }

            YatRecordType.XMR_STANDARD_ADDRESS -> "monero:${value.address}"
            else -> value.address
        }

        companion object {
            val names = YatRecordType.entries.associateBy { getSerializedName(it.javaClass.getField(it.name)) }

            private fun getSerializedName(enumField: Field): String? {
                if (enumField.isAnnotationPresent(SerializedName::class.java)) {
                    val fieldEnrich = enumField.getAnnotation(SerializedName::class.java)
                    return fieldEnrich?.value
                }
                return ""
            }
        }
    }
}