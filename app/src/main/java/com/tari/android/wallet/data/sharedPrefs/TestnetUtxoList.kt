package com.tari.android.wallet.data.sharedPrefs

import com.tari.android.wallet.service.faucet.TestnetTariUTXOKey

class TestnetUtxoList() : ArrayList<TestnetTariUTXOKey>() {
    constructor(e: List<TestnetTariUTXOKey>) : this() {
        addAll(e)
    }
}

fun TestnetUtxoList?.orEmpty(): TestnetUtxoList = this ?: TestnetUtxoList()