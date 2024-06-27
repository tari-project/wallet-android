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
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.lib.YatConfiguration
import yat.android.lib.YatIntegration
import yat.android.lib.YatLibApi
import yat.android.sdk.models.PaymentAddressResponse
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionData
import java.io.Serializable
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

    fun searchTariYats(emojiId: String): PaymentAddressResponse? =
        kotlin.runCatching { YatLibApi.emojiIDApi.lookupEmojiIDPayment(emojiId, TYPE_XTR) }.getOrNull()

    fun searchAnyYats(emojiId: String): PaymentAddressResponse? =
        kotlin.runCatching { YatLibApi.emojiIDApi.lookupEmojiIDPayment(emojiId, null) }.getOrNull()

    fun openOnboarding(context: Context) {
        val address = commonRepository.publicKeyHexString.orEmpty()
        YatIntegration.showOnboarding(context, listOf(YatRecord(YatRecordType.XTR_ADDRESS, data = address)))
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
        intent.putExtra(KEY_YATLIB_DATA, data as Serializable)
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

    companion object {
        private const val KEY_YATLIB_DATA = "YatLibDataKey"
        private const val TYPE_XTR = "0x0103"
    }
}