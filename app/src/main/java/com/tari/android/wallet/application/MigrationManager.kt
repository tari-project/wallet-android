package com.tari.android.wallet.application

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.walletManager.WalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(
    private val walletManager: WalletManager,
) {
    suspend fun validateVersion(onValid: () -> Unit, onError: () -> Unit) = withContext(Dispatchers.IO) {
        val walletVersion = walletManager.getLastAccessedToDbVersion()
            .replace("v", "")
            .takeIf { it.isNotEmpty() }
            ?.let { DefaultArtifactVersion(it) }
        val minValidVersion = DefaultArtifactVersion(BuildConfig.LIB_WALLET_MIN_VALID_VERSION.replace("v", ""))

        if (walletVersion == null || walletVersion < minValidVersion) {
            onError()
        } else {
            onValid()
        }
    }
}
