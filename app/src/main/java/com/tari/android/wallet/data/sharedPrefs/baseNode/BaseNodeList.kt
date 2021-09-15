package com.tari.android.wallet.data.sharedPrefs.baseNode

class BaseNodeList(): ArrayList<BaseNodeDto>() {
    constructor(e: List<BaseNodeDto>) : this() {
        addAll(e)
    }
}

fun BaseNodeList?.orEmpty() : BaseNodeList = this ?: BaseNodeList()