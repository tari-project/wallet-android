package com.tari.android.wallet.data.sharedPrefs.exolix

import android.content.SharedPreferences
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExolixPrefRepository @Inject constructor(
    sharedPreferences: SharedPreferences,
    networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private object Key {
        const val TRANSACTIONS = "exolix_transactions"
    }

    private var transactionList: ExolixTransactionList by SharedPrefGsonDelegate(
        prefs = sharedPreferences,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.TRANSACTIONS),
        type = ExolixTransactionList::class.java,
        defValue = ExolixTransactionList(),
    )

    var transactions: List<Exolix.Transaction>
        get() = transactionList
        set(value) {
            transactionList = ExolixTransactionList(value)
        }

    fun saveTransaction(transaction: Exolix.Transaction) {
        val currentTransactions = transactions.toMutableList()
        val existingIndex = currentTransactions.indexOfFirst { it.id == transaction.id }

        if (existingIndex >= 0) {
            currentTransactions[existingIndex] = transaction
        } else {
            currentTransactions.add(transaction)
        }

        transactions = currentTransactions
    }

    fun removeTransaction(transactionId: String) {
        transactions = transactions.filterNot { it.id == transactionId }
    }

    fun clear() {
        transactions = emptyList()
    }
}

private class ExolixTransactionList(transactions: List<Exolix.Transaction>) : ArrayList<Exolix.Transaction>(transactions) {
    constructor() : this(emptyList())
}
