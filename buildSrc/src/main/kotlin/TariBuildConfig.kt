object TariBuildConfig {

    const val versionNumber = "1.1.1"

    const val minSdk = 26
    const val targetSdk = 34
    const val compileSdk = 35

    object LibWallet {
        // We use different versions of the library for different networks, set $network to easily switch between them
        val network: LibWalletNetwork = LibWalletNetwork.MAINNET

        val version = when (network) {
            LibWalletNetwork.MAINNET -> "v3.0.0"
            LibWalletNetwork.NEXTNET -> "v3.0.0-rc.3"
        }
        val minValidVersion = when (network) {
            LibWalletNetwork.MAINNET -> "v0.0.0"
            LibWalletNetwork.NEXTNET -> "v1.18.0-rc.0"
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

        enum class LibWalletNetwork { MAINNET, NEXTNET }
    }
}