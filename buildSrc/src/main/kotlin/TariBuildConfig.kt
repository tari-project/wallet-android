object TariBuildConfig {

    const val versionNumber = "1.2.0"

    const val minSdk = 26
    const val targetSdk = 34
    const val compileSdk = 35

    object LibWallet {
        // We use different versions of the library for different networks, set $network to easily switch between them
        val network: LibWalletNetwork = LibWalletNetwork.MAINNET

        private const val LIB_VERSION = "v4.5.0"
        val version = when (network) {
            LibWalletNetwork.MAINNET -> LIB_VERSION
            LibWalletNetwork.NEXTNET -> "$LIB_VERSION-rc.0"
            LibWalletNetwork.ESMERALDA -> "$LIB_VERSION-pre.0"
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