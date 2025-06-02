package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.model.TariWalletAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeeplinkParser @Inject constructor(private val networkRepository: NetworkPrefRepository) {

    fun parse(deepLinkUri: Uri): DeepLink? {
        // Try to parse the URI as a pure Tari address (e.g. the QR code from Safe Trade scan)
        val walletAddress = TariWalletAddress.makeTariAddressOrNull(deepLinkUri.toString())
        if (walletAddress != null) {
            return DeepLink.UserProfile(tariAddress = walletAddress.fullBase58)
        }

        val torBridges = getTorDeeplink(deepLinkUri.toString().trim())
        if (torBridges.isNotEmpty()) {
            return DeepLink.TorBridges(torBridges)
        }

        if (!deepLinkUri.authority.equals(networkRepository.currentNetwork.network.uriComponent)) {
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
        }
    }

    fun toDeeplink(deepLink: DeepLink): String {
        if (deepLink is DeepLink.TorBridges) {
            return deepLink.torConfigurations.joinToString("\n") {
                "${it.ip}:${it.port} ${it.fingerprint}"
            }
        }

        val fullPart = Uri.Builder()
            .scheme(SCHEME)
            .authority(networkRepository.currentNetwork.network.uriComponent)
            .appendPath(deepLink.getCommand())

        deepLink.getParams().forEach { (key, value) ->
            fullPart.appendQueryParameter(key, value)
        }

        return fullPart.build().toString()
    }

    private fun getTorDeeplink(input: String): List<TorBridgeConfiguration> {
        return REGEX.findAll(input).mapNotNull { match ->
            try {
                val ipAddressAndPort = match.groupValues[1].split(":")
                val sha1Hash = match.groupValues[2]
                TorBridgeConfiguration("", ipAddressAndPort[0], ipAddressAndPort[1], sha1Hash)
            } catch (e: Exception) {
                null
            }
        }.toList()
    }

    companion object {
        const val SCHEME = "tari"
        val REGEX = Regex("""(\d+\.\d+\.\d+\.\d+:\d+) ([0-9A-Fa-f]+)""")
    }
}