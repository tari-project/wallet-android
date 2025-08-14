object TariBuildConfig {

    const val versionNumber = "1.3.0"

    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35

    object LibWallet {
        // We use different versions of the library for different networks, set $network to easily switch between them
        val network: LibWalletNetwork = LibWalletNetwork.MAINNET

        val version = when (network) {
            LibWalletNetwork.MAINNET -> "v4.10.0"
            LibWalletNetwork.NEXTNET -> "v4.9.0-rc.0"
            LibWalletNetwork.ESMERALDA -> "v4.9.0-pre.1"
        }
        val minValidVersion = when (network) {
            LibWalletNetwork.MAINNET -> "v0.0.0"
            LibWalletNetwork.NEXTNET -> "v4.0.0-rc.0"
            LibWalletNetwork.ESMERALDA -> "v4.0.0-pre.0"
        }

        const val hostURL = "https://github.com/tari-project/tari/releases/download/"
        const val x64A = "libminotari_wallet_ffi.android_x86_64.a"
        const val armA = "libminotari_wallet_ffi.android_aarch64.a"
        const val header = "libminotari_wallet_ffi.h"

        val headerFileUrl
            get() = "${hostURL}${version}/${header}"
        val armAFileUrl
            get() = "${hostURL}${version}/${armA}"
        val x64AFileUrl
            get() = "${hostURL}${version}/${x64A}"

        enum class LibWalletNetwork { MAINNET, NEXTNET, ESMERALDA }
    }
}