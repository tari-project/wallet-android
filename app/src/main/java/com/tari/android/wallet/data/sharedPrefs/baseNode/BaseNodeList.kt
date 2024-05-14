package com.tari.android.wallet.data.sharedPrefs.baseNode

class BaseNodeList(baseNodes: List<BaseNodeDto>) : ArrayList<BaseNodeDto>(baseNodes) {
    constructor() : this(emptyList())
}

fun BaseNodeList?.orEmpty(): BaseNodeList = this ?: BaseNodeList(emptyList())