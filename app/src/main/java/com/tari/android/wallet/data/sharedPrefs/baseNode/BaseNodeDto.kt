package com.tari.android.wallet.data.sharedPrefs.baseNode

import java.io.Serializable

data class BaseNodeDto(val name: String, val publicKeyHex: String, val address: String, val isCustom: Boolean = false) : Serializable