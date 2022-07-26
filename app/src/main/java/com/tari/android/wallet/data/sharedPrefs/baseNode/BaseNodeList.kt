package com.tari.android.wallet.data.sharedPrefs.baseNode

class BaseNodeList : ArrayList<BaseNodeDto>()

fun BaseNodeList?.orEmpty() : BaseNodeList = this ?: BaseNodeList()