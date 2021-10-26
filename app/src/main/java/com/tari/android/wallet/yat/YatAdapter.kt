package com.tari.android.wallet.yat

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import yat.android.api.LookupEmojiIdWithSymbolResponse
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.lib.YatConfiguration
import yat.android.lib.YatIntegration

class YatAdapter(
    private val yatSharedRepository: YatSharedRepository,
    private val commonRepository: SharedPrefsRepository
) : YatIntegration.Delegate {
    fun initYat() {
        val config = YatConfiguration(BuildConfig.YAT_ORGANIZATION_RETURN_URL, BuildConfig.YAT_ORGANIZATION_NAME, BuildConfig.YAT_ORGANIZATION_KEY)
        YatIntegration.setup(config, YatIntegration.ColorMode.LIGHT, this)
    }

    suspend fun searchYats(query: String) : LookupEmojiIdWithSymbolResponse = YatIntegration.yatApi.lookupEmojiIdWithSymbol(query, "XTR")

    fun openOnboarding(context: Context) {
        val address = commonRepository.publicKeyHexString.orEmpty()
        YatIntegration.showOnboarding(context, listOf(YatRecord(YatRecordType.TARI_PUBKEY, data = address)))
    }

    override fun onYatIntegrationComplete(yat: String) {
        Logger.d("Yat integration completed.")
        yatSharedRepository.saveYat(yat)
    }

    override fun onYatIntegrationFailed(failureType: YatIntegration.FailureType) {
        Logger.d("Yat integration failed.$failureType")
    }
}