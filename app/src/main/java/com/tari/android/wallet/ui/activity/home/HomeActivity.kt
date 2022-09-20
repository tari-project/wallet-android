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
package com.tari.android.wallet.ui.activity.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.User
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.service.connection.TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.gyphy.GiphyEcosystem
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.showInternetConnectionErrorDialog
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.fragment.send.activity.SendTariActivity
import com.tari.android.wallet.ui.fragment.tx.TxListFragmentDirections
import com.tari.android.wallet.ui.fragment.tx.TxListRouter
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject


class HomeActivity : CommonActivity<ActivityHomeBinding, HomeViewModel>(), TxListRouter {

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
    lateinit var giphy: GiphyEcosystem

    private val deeplinkViewModel: DeeplinkViewModel by viewModels()

    private val navController by lazy { Navigation.findNavController(this, R.id.nav_host_fragment) }

    private var appBarConfiguration =
        AppBarConfiguration(setOf(R.id.txListFragment, R.id.ttlStoreFragment, R.id.profileFragment, R.id.settingsFragment))

    private lateinit var serviceConnection: TariWalletServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)

        val viewModel: HomeViewModel by viewModels()
        bindViewModel(viewModel)

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
        serviceConnection = ViewModelProvider(this)[TariWalletServiceConnection::class.java]
        ui = ActivityHomeBinding.inflate(layoutInflater).also { setContentView(it.root) }

        if (savedInstanceState == null) {
            giphy.enable()
            serviceConnection.connection.subscribe {
                if (it.status == CONNECTED) {
                    ui.root.postDelayed({
                        processIntentDeepLink(it.service!!, intent)
                    }, Constants.UI.mediumDurationMs)
                }
            }.addTo(viewModel.compositeDisposable)
        }

        NavigationUI.setupWithNavController(ui.bottomNavigationView, navController)

        setupUi()
        Handler(Looper.getMainLooper()).postDelayed(
            { checkNetworkCompatibility() },
            3000
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // onNewIntent might get called before onCreate, so we anticipate that here
        checkScreensDeeplink(intent)
        if (::serviceConnection.isInitialized && serviceConnection.currentState.status == CONNECTED) {
            processIntentDeepLink(serviceConnection.currentState.service!!, intent)
        } else {
            setIntent(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean = NavigationUI.navigateUp(navController, appBarConfiguration)

    override fun toTTLStore() {
        ui.bottomNavigationView.selectedItemId = R.id.ttlStoreFragment
    }

    override fun toAllSettings() {
        ui.bottomNavigationView.selectedItemId = R.id.settingsFragment
    }

    fun willNotifyAboutNewTx(): Boolean = ui.bottomNavigationView.selectedItemId == R.id.txListFragment

    private fun setupUi() = setupCTAs()

    private fun setupCTAs() {
        ui.sendTariCtaView.setOnClickListener {
            if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
                showInternetConnectionErrorDialog(this)
            } else {
                startActivity(Intent(this, SendTariActivity::class.java))
                overridePendingTransition(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left
                )
            }
        }
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
                CustomFont.AVENIR_LT_STD_MEDIUM,
                listOf(
                    string(R.string.incompatible_network_description_bold_part_1),
                    string(R.string.incompatible_network_description_bold_part_2)
                ),
                CustomFont.AVENIR_LT_STD_BLACK
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
                    (intent.getParcelableExtra<TxId>(HomeDeeplinkScreens.KeyTxDetailsArgs))?.let { toTxDetails(it) }
                }
                else -> {}
            }
        }
    }

    private fun toTxDetails(txId: TxId?) = navigator.navigate(TxListFragmentDirections.toTxDetailsFragment().setTxId(txId))

    private fun processIntentDeepLink(service: TariWalletService, intent: Intent) {
        deeplinkHandler.handle(intent.data?.toString().orEmpty())?.let { deepLink ->
            (deepLink as? DeepLink.Send)?.let { sendTariToUser(service, it) }

            (deepLink as? DeepLink.AddBaseNode)?.let { deeplinkViewModel.executeAction(this, it) }
        }
    }

    private fun sendTariToUser(service: TariWalletService, sendDeeplink: DeepLink.Send) {
        val error = WalletError()
        val contacts = service.getContacts(error)
        val pubKey = service.getPublicKeyFromHexString(sendDeeplink.publicKeyHex)
        val recipientUser = when (error) {
            WalletError.NoError -> contacts.firstOrNull { it.publicKey == pubKey } ?: User(pubKey)
            else -> User(pubKey)
        }
        val intent = Intent(this, SendTariActivity::class.java)
        intent.putExtra("recipientUser", recipientUser as Parcelable)
        sendDeeplink.note.let { intent.putExtra(SendTariActivity.PARAMETER_NOTE, it) }
        sendDeeplink.amount?.let { intent.putExtra(SendTariActivity.PARAMETER_AMOUNT, it.tariValue) }
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = WeakReference(null)
        viewModelStore.clear()
    }

    companion object {
        var instance = WeakReference<HomeActivity>(null)
            private set
    }
}
