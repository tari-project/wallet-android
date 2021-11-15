package com.tari.android.wallet.data.sharedPrefs.testnetFaucet

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBigIntegerDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import java.math.BigInteger

class TestnetFaucetRepository(private val sharedPrefs: SharedPreferences, private val networkRepository: NetworkRepository) {

    var faucetTestnetTariRequestCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Keys.faucetTestnetTariRequestCompleted))

    var testnetTariUTXOKeyList: TestnetUtxoList? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.testnetTariUTXOListKey), TestnetUtxoList::class.java)

    var firstTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, formatKey(Keys.firstTestnetUTXOTxId))

    var secondTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, formatKey(Keys.secondTestnetUTXOTxId))

    init {
        // for migration purposes, to avoid a second redundant faucet call:
        // faucetTestnetTariRequestCompleted was introduced  after firstTestnetUTXOTxId and
        // secondTestnetUTXOTxId properties
        if (firstTestnetUTXOTxId != null && secondTestnetUTXOTxId != null) {
            faucetTestnetTariRequestCompleted = true
        }
    }

    private fun formatKey(key: String): String = key + "_" + networkRepository.currentNetwork!!.network.displayName

    fun clear() {
        faucetTestnetTariRequestCompleted = false
        testnetTariUTXOKeyList = null
        firstTestnetUTXOTxId = null
        secondTestnetUTXOTxId = null
    }

    object Keys {
        const val faucetTestnetTariRequestCompleted = "tari_wallet_faucet_testnet_tari_request_completed"
        const val testnetTariUTXOListKey = "tari_wallet_testnet_tari_utxo_key_list"
        const val firstTestnetUTXOTxId = "tari_wallet_first_testnet_utxo_tx_id"
        const val secondTestnetUTXOTxId = "tari_wallet_second_testnet_utxo_tx_id"
    }
}