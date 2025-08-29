object TariBuildConfig {

    const val versionNumber = "1.4.1"

    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35

    object LibWallet {
        val version = "v5.0.0-pre.8"
        val minValidVersion = "v0.0.0" // Always valid. Probably, need to remove the check in the future.

        enum class LibWalletNetwork { MAINNET, ESMERALDA }
    }
}