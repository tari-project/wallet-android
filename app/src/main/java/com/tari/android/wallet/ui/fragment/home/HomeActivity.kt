/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.service.connection.ServiceConnectionStatus
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.INDEX_CONTACT_BOOK
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.INDEX_HOME
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.INDEX_SETTINGS
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.INDEX_STORE
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.NO_SMOOTH_SCROLL
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.splash.SplashActivity
import com.tari.android.wallet.ui.fragment.store.StoreFragment
import com.tari.android.wallet.ui.fragment.tx.HomeFragment
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

class HomeActivity : CommonActivity<ActivityHomeBinding, HomeViewModel>() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var migrationManager: MigrationManager

    @Inject
    lateinit var tariSettingsRepository: TariSettingsSharedRepository

    private val deeplinkViewModel: DeeplinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)

        val viewModel: HomeViewModel by viewModels()
        bindViewModel(viewModel)
        subscribeToCommon(deeplinkViewModel)

        subscribeToCommon(viewModel.shareViewModel)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothServer)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothClient)
        subscribeToCommon(viewModel.shareViewModel.deeplinkViewModel)
        viewModel.nfcAdapter.context = this

        viewModel.shareViewModel.tariBluetoothServer.init(this)
        viewModel.shareViewModel.tariBluetoothClient.init(this)

        setContainerId(R.id.nav_container)

        overridePendingTransition(0, 0)
        appComponent.inject(this)
        if (!sharedPrefsWrapper.isAuthenticated) {
            val intent = Intent(this, SplashActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            this.intent?.data?.let(intent::setData)
            startActivity(intent)
            finish()
            return
        }
        ui = ActivityHomeBinding.inflate(layoutInflater).also { setContentView(it.root) }
        if (savedInstanceState == null) {
            enableNavigationView(ui.homeImageView)
            viewModel.doOnConnected {
                ui.root.postDelayed({
                    processIntentDeepLink(intent)
                }, Constants.UI.mediumDurationMs)
            }
        } else {
            val index = savedInstanceState.getInt(KEY_PAGE)
            ui.viewPager.setCurrentItem(index, false)
            enableNavigationView(index)
        }
        setupUi()
        subscribeUI()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000)
            launch(Dispatchers.Main) {
                checkNetworkCompatibility()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.shareViewModel.tariBluetoothServer.init(this)

        viewModel.nfcAdapter.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.nfcAdapter.disableForegroundDispatch(this)
    }

    private fun subscribeUI() = with(viewModel) {
        observe(shareViewModel.shareText) { shareViaText(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.shareViewModel.tariBluetoothServer.handleActivityResult(requestCode, resultCode, data)
        viewModel.shareViewModel.tariBluetoothClient.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        viewModel.nfcAdapter.onNewIntent(intent)

        // onNewIntent might get called before onCreate, so we anticipate that here
        checkScreensDeeplink(intent)
        if (viewModel.serviceConnection.currentState.status == ServiceConnectionStatus.CONNECTED) {
            processIntentDeepLink(intent)
        } else {
            setIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, ui.viewPager.currentItem)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            if (ui.viewPager.currentItem == INDEX_HOME) {
                super.onBackPressed()
            } else {
                ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
                enableNavigationView(ui.homeImageView)
            }
        }
    }

    fun setBottomBarVisibility(isVisible: Boolean) {
        val postDelay = if (!isVisible) 0 else Constants.UI.shortDurationMs
        ui.bottomNavigationView.postDelayed({
            ui.bottomNavigationView.setVisible(isVisible)
            ui.sendTariButton.setVisible(isVisible)
        }, postDelay)
    }

    private fun setupUi() {
        ui.sendTariButton.setOnClickListener { viewModel.navigation.postValue(Navigation.ContactBookNavigation.ToSelectTariUser()) }
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        enableNavigationView(ui.homeImageView)
        ui.viewPager.adapter = HomeAdapter(supportFragmentManager, this.lifecycle)
        ui.viewPager.isUserInputEnabled = false
        ui.viewPager.offscreenPageLimit = 3
        ui.homeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.homeImageView)
        }
        ui.storeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_STORE, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.storeImageView)
        }
        ui.walletInfoView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_CONTACT_BOOK, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.walletInfoImageView)
        }
        ui.settingsView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.settingsImageView)
        }
    }

    private fun enableNavigationView(index: Int) {
        val view: ImageView = when (index) {
            INDEX_HOME -> ui.homeImageView
            INDEX_STORE -> ui.storeImageView
            INDEX_CONTACT_BOOK -> ui.walletInfoImageView
            INDEX_SETTINGS -> ui.settingsImageView
            else -> error("Unexpected index: $index")
        }
        enableNavigationView(view)
    }

    private fun enableNavigationView(view: ImageView) {
        arrayOf(ui.homeImageView, ui.storeImageView, ui.walletInfoImageView, ui.settingsImageView).forEach { it.clearColorFilter() }
        view.setColorFilter(viewModel.paletteManager.getPurpleBrand(this))
    }

    private fun checkNetworkCompatibility() {
        if (!networkRepository.supportedNetworks.contains(networkRepository.currentNetwork!!.network) && !networkRepository.incompatibleNetworkShown) {
            networkRepository.incompatibleNetworkShown = true
            displayIncompatibleNetworkDialog()
        }
    }

    private fun displayIncompatibleNetworkDialog() {
        if (this.isFinishing) return

        val description = string(R.string.incompatible_network_description)
            .applyFontStyle(
                this@HomeActivity,
                TariFont.AVENIR_LT_STD_MEDIUM,
                listOf(
                    string(R.string.incompatible_network_description_bold_part_1),
                    string(R.string.incompatible_network_description_bold_part_2)
                ),
                TariFont.AVENIR_LT_STD_BLACK
            )
        val dialog = ModularDialog(this)
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = false), modules = listOf(
                HeadModule(string(R.string.incompatible_network_title)),
                BodyModule(null, description),
                ButtonModule(string(R.string.incompatible_network_reset_now), ButtonStyle.Normal) {
                    deleteWallet()
                    dialog.dismiss()
                },
                ButtonModule(string(R.string.incompatible_network_reset_later), ButtonStyle.Close)
            )
        )
        dialog.applyArgs(args)
        dialog.show()
    }

    private fun deleteWallet() {
        // delete wallet
        goToSplashScreen()
        lifecycleScope.launch(Dispatchers.IO) {
            walletServiceLauncher.stopAndDelete()
        }
    }

    private fun goToSplashScreen() {
        val intent = Intent(this, OnboardingFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun checkScreensDeeplink(intent: Intent) {
        val screen = intent.getStringExtra(HomeDeeplinkScreens.Key)
        if (screen.orEmpty().isNotEmpty()) {
            when (HomeDeeplinkScreens.parse(screen)) {
                HomeDeeplinkScreens.TxDetails -> {
                    (intent.parcelable<TxId>(HomeDeeplinkScreens.KeyTxDetailsArgs))?.let { viewModel.tariNavigator.toTxDetails(null, it) }
                }

                else -> {}
            }
        }
    }

    fun willNotifyAboutNewTx(): Boolean = ui.viewPager.currentItem == INDEX_HOME

    private fun processIntentDeepLink(intent: Intent) {
        deeplinkViewModel.tryToHandle(intent.data?.toString().orEmpty())
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = WeakReference(null)
        viewModelStore.clear()
    }

    class HomeAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

        override fun createFragment(position: Int): Fragment = when (position) {
            INDEX_HOME -> HomeFragment()
            INDEX_STORE -> StoreFragment.newInstance()
            INDEX_CONTACT_BOOK -> ContactBookFragment()
            INDEX_SETTINGS -> AllSettingsFragment.newInstance()
            else -> error("Unexpected position: $position")
        }

        override fun getItemCount(): Int = 4
    }

    companion object {

        private const val KEY_PAGE = "key_page"

        @Volatile
        var instance: WeakReference<HomeActivity> = WeakReference(null)
            private set
    }
}


