object TariBuildConfig {

    const val versionNumber = "1.3.0"

    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35

    object LibWallet {
        val version = "v4.10.0"
        val minValidVersion = "v0.0.0" // Always valid. Probably, need to remove the check in the future.

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

        enum class LibWalletNetwork { MAINNET, ESMERALDA }
    }
}