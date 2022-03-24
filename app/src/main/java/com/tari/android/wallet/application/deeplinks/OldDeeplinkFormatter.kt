package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.model.MicroTari
import java.math.BigInteger
import java.net.URLDecoder

@Deprecated("Delete after 01.10.2022")
class OldDeeplinkFormatter() {

    private val regexPublicKeyHex = "pubkey" + "/([a-zA-Z0-9]{64})"

    fun from(networkRepository: NetworkRepository, deepLink: String): DeepLink? {
        val regexNetwork = "(" + Network.MAINNET.uriComponent + "|" + networkRepository.currentNetwork!!.network.uriComponent + ")"

        val publicKeyRegex = Regex("tari://$regexNetwork/$regexPublicKeyHex(\\?.*)?")

        if (publicKeyRegex.matches(deepLink)) {
            val matchResult = publicKeyRegex.find(deepLink)!!
            val (networkUriComponent, value, parameters) = matchResult.destructured
            if (!networkRepository.currentNetwork!!.network.uriComponent.equals(networkUriComponent)) {
                return null
            }
            val parsedParameters = parseParameters(parameters)
            val note = parsedParameters[DeepLink.Send.noteKey].orEmpty()
            val amount = parsedParameters[DeepLink.Send.amountKey]?.let { MicroTari(BigInteger(it)) }
            return DeepLink.Send(value, amount, note)
        }
        return null
    }

    private fun parseParameters(parameters: String): Map<String, String> =
        if (parameters.length <= 1)
            emptyMap()
        else parameters.substring(1)
            .split('=', '&')
            .windowed(2, 2)
            .associate { it.first() to URLDecoder.decode(it.last(), "UTF-8") }
}