package com.tari.android.wallet.ui.screen.home

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.ui.common.CommonXmlActivity
import com.tari.android.wallet.ui.compose.TariDesignSystem
import kotlin.getValue

class HomeActivity : CommonXmlActivity<ActivityHomeBinding, HomeViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ui = ActivityHomeBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: HomeViewModel by viewModels()
        bindViewModel(viewModel)

        // TODO shareViewModel isn't a real view model!!
        subscribeToCommon(viewModel.shareViewModel)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothServer)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothClient)

        setContainerId(R.id.nav_container)

        enableEdgeToEdge()

        ui.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                TariDesignSystem(viewModel.currentTheme) {
                    HomeScreen(
                        uiState = uiState,
                        fragmentManager = supportFragmentManager,
                        onMenuItemClicked = { viewModel.onMenuItemClicked(it) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.securityPrefRepository.isAuthenticated) {
            viewModel.navigateToAuth(this.intent.data)
            finish()
        }
    }
}