package com.tari.android.wallet.application

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.rwa.RwaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(
    private val walletManager: WalletManager,
    private val rwaRepository: RwaRepository,
) {
    suspend fun validateVersion(
        onValid: () -> Unit,
        onError: (error: VersionError) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val walletVersion = getWalletLibVersion()
        val minValidVersion = getMinimumWalletLibVersion()

        val appVersion = DefaultArtifactVersion(BuildConfig.VERSION_NAME.replace("v", "").replace("-", "."))
        val (minAppVersion, recommendedAppVersion) = runCatching {
            rwaRepository.getMobileVersion()
                .let { DefaultArtifactVersion(it.minAndroidVersion) to DefaultArtifactVersion(it.recommendedAndroidVersion) }
        }.getOrElse { null to null }

        when {
            walletVersion == null || walletVersion < minValidVersion -> onError(VersionError.IncompatibleLib)
            minAppVersion != null && appVersion < minAppVersion -> onError(VersionError.MandatoryUpdate)
            recommendedAppVersion != null && appVersion < recommendedAppVersion -> onError(VersionError.RecommendedUpdate)
            else -> onValid()
        }
    }

    private fun getWalletLibVersion(): DefaultArtifactVersion? = walletManager.getLastAccessedToDbVersion()
        .replace("v", "")
        .replace("-", ".")
        .takeIf { it.isNotEmpty() }
        ?.let { DefaultArtifactVersion(it) }

    private fun getMinimumWalletLibVersion(): DefaultArtifactVersion =
        DefaultArtifactVersion(BuildConfig.LIB_WALLET_MIN_VALID_VERSION.replace("v", "").replace("-", "."))

    sealed class VersionError() {
        data object IncompatibleLib : VersionError()
        data object RecommendedUpdate : VersionError()
        data object MandatoryUpdate : VersionError()
    }
}
