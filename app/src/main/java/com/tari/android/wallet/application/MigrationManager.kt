package com.tari.android.wallet.application

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(
    private val walletManager: WalletManager,
    private val application: TariWalletApplication,
    private val networkRepository: NetworkPrefRepository,
) {

    suspend fun validateVersion(onValid: () -> Unit, onError: () -> Unit) {
        val walletVersion = getCurrentWalletVersion()

        // FIXME: Temporary migration mechanism. It should be removed when minValidVersion is greater than 1.0.0-rc.8
        if (walletVersion.isNotEmpty() && DefaultArtifactVersion(walletVersion) < DefaultArtifactVersion(PEER_DB_MIGRATION_MIN_VERSION)) {
            performPeerDbMigration()
        }

        if (walletVersion.isEmpty() || DefaultArtifactVersion(walletVersion) < DefaultArtifactVersion(BuildConfig.LIB_WALLET_MIN_VALID_VERSION)) {
            onError()
        } else {
            onValid()
        }
    }

    private suspend fun getCurrentWalletVersion(): String = withContext(Dispatchers.IO) { walletManager.getCommsConfig().getLastVersion() }

    private suspend fun performPeerDbMigration() = withContext(Dispatchers.IO) {
        application.applicationInfo.dataDir?.let { appDir ->
            val peerDbFile = File(appDir, "files/${networkRepository.currentNetwork.network.uriComponent}/data.mdb")
            if (peerDbFile.exists()) {
                peerDbFile.delete()
            }
        }
    }

    companion object {
        const val PEER_DB_MIGRATION_MIN_VERSION = "1.0.0-rc.8"
    }
}
