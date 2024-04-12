package com.tari.android.wallet.data.sharedPrefs.baseNode

import com.tari.android.wallet.application.deeplinks.DeepLink
import java.io.Serializable

data class BaseNodeDto(
    val name: String,
    val publicKeyHex: String,
    val address: String = "",
    val isCustom: Boolean = false,
) : Serializable {

    override fun toString() = ("$name: ").takeIf { name.isNotEmpty() }.orEmpty() + "$publicKeyHex::$address"

    fun toDeeplink(): DeepLink.AddBaseNode = DeepLink.AddBaseNode(name, "${publicKeyHex}::${address}")

    companion object {
        fun fromDeeplink(deeplink: DeepLink.AddBaseNode): BaseNodeDto {
            val (hex, address) = deeplink.peer.split("::")
            return BaseNodeDto(deeplink.name, hex, address, true)
        }
    }
}