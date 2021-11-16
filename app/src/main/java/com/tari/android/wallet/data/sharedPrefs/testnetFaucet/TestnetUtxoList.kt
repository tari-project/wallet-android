package com.tari.android.wallet.data.sharedPrefs.testnetFaucet

class TestnetUtxoList() : ArrayList<TestnetTariUTXOKey>() {
    constructor(e: List<TestnetTariUTXOKey>) : this() {
        addAll(e)
    }
}

fun TestnetUtxoList?.orEmpty(): TestnetUtxoList = this ?: TestnetUtxoList()