object TariBuildConfig {

    const val versionNumber = "1.5.0"

    const val minSdk = 26
    const val targetSdk = 35
    const val compileSdk = 35

    object LibWallet {
        val version = "v5.1.0"
        val minValidVersion = "v0.0.0" // Always valid. Probably, need to remove the check in the future.

        enum class LibWalletNetwork { MAINNET, ESMERALDA }
    }
}