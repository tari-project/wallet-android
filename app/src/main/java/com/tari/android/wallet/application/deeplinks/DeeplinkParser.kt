package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.model.TariWalletAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeeplinkParser @Inject constructor(private val networkRepository: NetworkPrefRepository) {

    fun parse(deepLinkUri: Uri): DeepLink? {
        val rawValue = deepLinkUri.toString()

        // Try to parse the URI as a pure Tari address (e.g. the QR code from Safe Trade scan)
        val walletAddress = TariWalletAddress.makeTariAddressOrNull(rawValue)
        if (walletAddress != null) {
            return DeepLink.UserProfile(tariAddress = walletAddress.fullBase58)
        }

        if (deepLinkUri.authority != null && deepLinkUri.authority != networkRepository.currentNetwork.network.uriComponent) {
            // Returns null because the deep link is valid, but is for a different network
            return null
        }

        val command = deepLinkUri.path.orEmpty().trimStart('/')
        val parameters = if (command == DeepLink.Contacts.COMMAND_CONTACTS) { // list params
            deepLinkUri.query.orEmpty().split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }
        } else {
            deepLinkUri.queryParameterNames.associateWith { deepLinkUri.getQueryParameter(it).orEmpty() }
        }

        return DeepLink.getByCommand(command, parameters)?.takeIf {
            when (it) {
                is DeepLink.Send -> TariWalletAddress.validateBase58(it.walletAddress)
                is DeepLink.UserProfile -> TariWalletAddress.validateBase58(it.tariAddress)
                else -> true // Handle other DeepLink types or consider returning null if they shouldn't be valid
            }
        } ?: DeepLink.Raw(rawValue) // Fallback to raw value if command not recognized
    }

    fun toDeeplink(deepLink: DeepLink): String {
        val fullPart = Uri.Builder()
            .scheme(SCHEME)
            .authority(networkRepository.currentNetwork.network.uriComponent)
            .appendPath(deepLink.getCommand())

        deepLink.getParams().forEach { (key, value) ->
            fullPart.appendQueryParameter(key, value)
        }

        return fullPart.build().toString()
    }

    companion object {
        const val SCHEME = "tari"
    }
}