package com.tari.android.wallet.ui.component.clipboardController

import android.content.ClipboardManager
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject

class WalletAddressViewModel() : CommonViewModel() {

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    val discoveredWalletAddress = SingleLiveEvent<TariWalletAddress>()

    init {
        component.inject(this)
    }

    fun tryToCheckClipboard() {
        doOnConnected {
            checkClipboardForValidEmojiId(it)
        }
    }

    fun checkClipboardForValidEmojiId(walletService: TariWalletService) {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return

        checkForValidEmojiId(walletService, clipboardString)
    }

    fun checkForValidEmojiId(walletService: TariWalletService, query: String) {
        val deepLink = deeplinkHandler.handle(query) as? DeepLink.Send
        if (deepLink != null) { // there is a deep link in the clipboard
            discoveredWalletAddress.value = walletService.getWalletAddressFromHexString(deepLink.walletAddressHex)
        } else { // try to extract a valid emoji id
            val emojis = query.trim().extractEmojis()
            // search in windows of length = emoji id length
            var currentIndex = emojis.size - Constants.Wallet.emojiIdLength
            while (currentIndex >= 0) {
                val emojiWindow = emojis
                    .subList(currentIndex, currentIndex + Constants.Wallet.emojiIdLength)
                    .joinToString(separator = "")
                // there is a chunked emoji id in the clipboard
                discoveredWalletAddress.value = walletService.getWalletAddressFromEmojiId(emojiWindow)
                if (discoveredWalletAddress.value != null) {
                    break
                }
                --currentIndex
            }
        }
        if (discoveredWalletAddress.value == null) {
            checkForWalletAddressHex(query)
        }
    }

    fun checkForWalletAddressHex(query: String): Boolean {
        val hexStringRegex = Regex("([A-Za-z0-9]{66})")
        var result = hexStringRegex.find(query)
        while (result != null) {
            val hexString = result.value
            discoveredWalletAddress.value = walletService.getWalletAddressFromHexString(hexString)
            if (discoveredWalletAddress.value != null) {
                return true
            }
            result = result.next()
        }
        return false
    }

    fun getWalletAddressFromHexString(publicKeyHex: String): TariWalletAddress? =
        walletService.getWithError { _, wallet -> wallet.getWalletAddressFromHexString(publicKeyHex) }

    fun getWalletAddressFromEmojiId(emojiId: String): TariWalletAddress? =
        walletService.getWithError { _, wallet -> wallet.getWalletAddressFromEmojiId(emojiId) }
}