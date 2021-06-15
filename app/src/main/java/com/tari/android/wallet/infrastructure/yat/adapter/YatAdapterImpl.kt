package com.tari.android.wallet.infrastructure.yat.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.infrastructure.yat.YatUser
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import yat.android.YatAppConfig
import yat.android.YatLib
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.data.response.SupportedEmojiSetResponse
import yat.android.data.response.YatLookupResponse
import java.util.*

class YatAdapterImpl (
    val yatUserStorage: YatUserStorage,
    val sharedPrefsWrapper: SharedPrefsWrapper,
    context: Context
) : YatAdapter, YatLib.Delegate {

    init {
        YatLib.initialize(context, this)
    }

    private val _state =
        MutableLiveData(YatAdapter.YatIntegrationStateDto(YatAdapter.YatIntegrationState.None))
    override val state: LiveData<YatAdapter.YatIntegrationStateDto> = Transformations.map(_state) { it }

    override fun processDeeplink(context: Context, intent: Intent) {
        intent.data?.let {
            YatLib.processDeepLink(context, it)
        }
    }

    override fun start(intent: Activity) {
        initializeYatLib()
        YatLib.start(intent)
    }

    override fun getJWTStorage() = YatLib.jwtStorage

    override suspend fun lookupYatUser(yat: String): YatAdapter.YatResponse<YatLookupResponse> =
        mapToYatResponse { onSuccess, onError -> YatLib.lookupYat(yat, onSuccess, onError) }

    override suspend fun getSupportedEmojiSet(): YatAdapter.YatResponse<SupportedEmojiSetResponse> =
        mapToYatResponse(YatLib.Companion::getSupportedEmojiSet)

    private suspend fun <T> mapToYatResponse(request: ((T) -> Unit, (Int?, Throwable?) -> Unit) -> Unit): YatAdapter.YatResponse<T> {
        var response: T? = null
        var throwable: Throwable? = null
        val job: CompletableJob = Job()

        request({
            response = it
            job.complete()
        }, { _, error ->
            throwable = error
            job.complete()
        })

        joinAll(job)

        return YatAdapter.YatResponse(response, throwable)
    }

    private fun initializeYatLib() {
        // library configuration
        val config = YatAppConfig(
            name = BuildConfig.YAT_APPNAME,
            sourceName = BuildConfig.YAT_SOURCE_NAME,
            pathKey = BuildConfig.YAT_PATH_KEY,
            pubKey = BuildConfig.YAT_PUBLIC_KEY,
            code = BuildConfig.YAT_CODE,
            authToken = BuildConfig.YAT_AUTH_TOKEN
        )

        val yatRecords = listOf(
            YatRecord(YatRecordType.TARI_PUBKEY, sharedPrefsWrapper.publicKeyHexString.orEmpty())
        )

        val yatUser = YatUser(
            UUID.randomUUID().toString().substring(0, 15),
            UUID.randomUUID().toString().substring(0, 15), setOf()
        )
        yatUserStorage.put(yatUser)

        YatLib.setup(
            config = config,
            userId = yatUser.alternateId,
            userPassword = yatUser.password,
            colorMode = YatLib.ColorMode.LIGHT,
            yatRecords = yatRecords
        )
    }

    override fun onYatIntegrationComplete(yat: String) {
        Logger.i("Yat integration complete")
        _state.postValue(
            YatAdapter.YatIntegrationStateDto(
                YatAdapter.YatIntegrationState.Complete,
                yat
            )
        )
    }

    override fun onYatIntegrationFailed(failureType: YatLib.FailureType) {
        Logger.e("Yat integration had error")
        _state.postValue(
            YatAdapter.YatIntegrationStateDto(
                YatAdapter.YatIntegrationState.Failed,
                null,
                failureType
            )
        )
    }
}