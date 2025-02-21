package com.tari.android.wallet.ui.screen.home

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.os.Build
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
import com.tari.android.wallet.ui.screen.StartActivity
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extension.observe
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
        viewModel.shareViewModel.tariBluetoothServer.init(this)
        viewModel.shareViewModel.tariBluetoothClient.init(this)

        setContainerId(R.id.nav_container)

        if (!viewModel.isAuthenticated) {
            val intent = Intent(this, StartActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            this.intent?.data?.let(intent::setData)
            finish()
            startActivity(intent)
            return
        }

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

        if (savedInstanceState == null) {
            viewModel.doOnWalletRunning {
                ui.root.postDelayed({ viewModel.processIntentDeepLink(this, intent) }, Constants.UI.mediumDurationMs)
            }
        }

        observe(viewModel.shareViewModel.shareText) { shareViaText(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(POST_NOTIFICATIONS), 0)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.securityPrefRepository.isAuthenticated) {
            viewModel.navigateToAuth(this.intent.data)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.shareViewModel.tariBluetoothServer.handleActivityResult(requestCode, resultCode, data)
        viewModel.shareViewModel.tariBluetoothClient.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // onNewIntent might get called before onCreate, so we anticipate that here
        setIntent(intent)
    }


    override fun onDestroy() {
        viewModel.onDestroy()
        super.onDestroy()
        viewModelStore.clear()
    }
}