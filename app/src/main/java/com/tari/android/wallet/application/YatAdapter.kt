package com.tari.android.wallet.application

import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxViewModel
import com.tari.android.wallet.ui.fragment.send.finalize.YatFinalizeSendTxActivity
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.EmojiId
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.lib.YatConfiguration
import yat.android.lib.YatIntegration
import yat.android.lib.YatLibApi
import yat.android.sdk.models.PaymentAddressResponseResult
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionData
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

    val isYatDisconnected: Boolean // TODO make a check every time we show the Wallet Info screen
        get() = yatSharedRepository.yatWasDisconnected

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
    suspend fun searchTariYat(yatEmojiId: EmojiId): PaymentAddressResponseResult? {
        return try {
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
    suspend fun searchAllYats(yatEmojiId: EmojiId): Map<String, PaymentAddressResponseResult> {
        return try {
            val result = YatLibApi.emojiIDApi.lookupEmojiIDPayment(yatEmojiId, null).result
            if (result.isNullOrEmpty()) logger.d("Can't find any Yats for $yatEmojiId")
            result.orEmpty()
        } catch (e: Exception) {
            logger.e("Error while searching for Yats for $yatEmojiId:\n${e.message}")
            emptyMap()
        }
    }

    fun openOnboarding(context: Context) {
        val address = commonRepository.walletAddressBase58.orEmpty()
        YatIntegration.showOnboarding(context, listOf(YatRecord(YatRecordType.XTM_ADDRESS, data = address)))
    }

    fun showOutcomingFinalizeActivity(activity: Activity, transactionData: TransactionData) {
        val yatUser = transactionData.recipientContact?.yatDto ?: return
        val currentTicker = networkRepository.currentNetwork.ticker
        val data = YatLibOutcomingTransactionData(
            amount = transactionData.amount!!.tariValue.toDouble(),
            currency = currentTicker,
            yat = yatUser.yat,
        )

        val intent = Intent(activity, YatFinalizeSendTxActivity::class.java)
        intent.putExtra(YatLibOutcomingTransactionActivity.DATA_KEY, data)
        intent.putExtra(FinalizeSendTxViewModel.KEY_TRANSACTION_DATA, transactionData)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    fun disconnectYat(disconnected: Boolean = true) {
        yatSharedRepository.yatWasDisconnected = disconnected
    }

    override fun onYatIntegrationComplete(yat: String) {
        logger.d("Yat integration completed with $yat")
        yatSharedRepository.saveYat(yat)
    }

    override fun onYatIntegrationFailed(failureType: YatIntegration.FailureType) {
        logger.d("Yat integration failed $failureType")
    }
}