object BuildConfig {
    const val agpVersion = "8.7.2"
    const val kotlinVersion = "2.0.21"

    const val lifecycleVersion = "2.8.7"
    const val coroutinesVersion = "1.9.0"

    // Build & version
    const val buildNumber = 319
    const val versionNumber = "0.30.0"

    object LibWallet {
        const val libwalletVersion = "v1.9.0-rc.0"
        const val libwalletMinValidVersion = "v1.4.1-rc.0"

        const val libwalletHostURL = "https://github.com/tari-project/tari/releases/download/"
        const val libwalletx64A = "libminotari_wallet_ffi.android_x86_64.a"
        const val libwalletArmA = "libminotari_wallet_ffi.android_aarch64.a"
        const val libwalletHeader = "libminotari_wallet_ffi.h"

        val headerFileUrl
            get() = "${libwalletHostURL}${libwalletVersion}/${libwalletHeader}"
        val armAFileUrl
            get() = "${libwalletHostURL}${libwalletVersion}/${libwalletArmA}"
        val x64AFileUrl
            get() = "${libwalletHostURL}${libwalletVersion}/${libwalletx64A}"
    }
}