@file:Suppress("ConstPropertyName")

object TariBuildConfig {

    const val versionNumber = "1.0.0"

    const val minSdk = 26
    const val targetSdk = 34
    const val compileSdk = 35

    object LibWallet {
        const val version = "v2.0.0-alpha.3"
        const val minValidVersion = "v2.0.0-alpha.3"

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
    }
}