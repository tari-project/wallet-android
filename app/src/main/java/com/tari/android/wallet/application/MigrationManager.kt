package com.tari.android.wallet.application

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.ui.common.SimpleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(private val manager: WalletManager) {

    private val simpleViewModel = SimpleViewModel()

    fun validateVersion(onValid: () -> Unit, onError: () -> Unit) {
        val walletVersion = getCurrentWalletVersion()

        if (walletVersion.isEmpty() || DefaultArtifactVersion(walletVersion) < DefaultArtifactVersion(BuildConfig.LIB_WALLET_MIN_VALID_VERSION)) {
            simpleViewModel.viewModelScope.launch(Dispatchers.Main) { onError() }
        } else {
            simpleViewModel.viewModelScope.launch(Dispatchers.Main) { onValid() }
        }
    }

    private fun getCurrentWalletVersion(): String = manager.getCommsConfig().getLastVersion()
}