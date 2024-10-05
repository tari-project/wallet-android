package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.model.TariWalletAddress
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DeeplinkParser @Inject constructor(private val networkRepository: NetworkPrefRepository) {

    fun parse(deepLink: String): DeepLink? {
        val torBridges = getTorDeeplink(deepLink)
        if (torBridges.isNotEmpty()) {
            return DeepLink.TorBridges(torBridges)
        }

        val uri = runCatching { Uri.parse(URLDecoder.decode(deepLink, "UTF-8")) }.getOrNull() ?: return null

        if (!uri.authority.equals(networkRepository.currentNetwork.network.uriComponent)) {
            return null
        }

        val command = uri.path.orEmpty().trimStart('/')
        val parameters = if (command == DeepLink.Contacts.COMMAND_CONTACTS || command == DeepLink.PaperWallet.COMMAND_PAPER_WALLET) { // list params
            uri.query.orEmpty().split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }
        } else {
            uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
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