package com.tari.android.wallet.data.sharedPrefs.exolix

import android.content.SharedPreferences
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExolixPrefRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private object Key {
        const val PENDING_TRANSACTION_DATA = "exolix_pending_transaction_data"
    }

    var pendingTransaction: Exolix.TransactionResponse? by SharedPrefGsonNullableDelegate(
        prefs = sharedPreferences,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.PENDING_TRANSACTION_DATA),
        type = Exolix.TransactionResponse::class.java,
    )

    fun savePendingTransaction(transaction: Exolix.TransactionResponse) {
        pendingTransaction = transaction
    }

    fun removePendingTransaction() {
        pendingTransaction = null
    }

    fun clear() {
        pendingTransaction = null
    }
}
