package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.util.extension.switchToIo
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

@Singleton
class AirdropRepository @Inject constructor(
    private val airdropRetrofit: AirdropRetrofitService,
) {

    @Throws(HttpException::class)
    suspend fun getMinerStats(): Int = switchToIo { airdropRetrofit.getMinerStats().totalMiners }
}