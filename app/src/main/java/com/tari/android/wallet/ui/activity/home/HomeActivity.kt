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
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.service.connection.TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.ui.activity.settings.BackupSettingsActivity
import com.tari.android.wallet.ui.activity.settings.DeleteWalletActivity
import com.tari.android.wallet.ui.activity.tx.TxDetailsActivity
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.gyphy.GiphyEcosystem
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomFontTextView
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.BaseNodeConfigRouter
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode.AddCustomBaseNodeFragment
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.fragment.send.activity.SendTariActivity
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsRouter
import com.tari.android.wallet.ui.fragment.settings.backgroundService.BackgroundServiceSettingsActivity
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.ui.fragment.tx.TxListRouter
import com.tari.android.wallet.util.Constants
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController


internal class HomeActivity : CommonActivity<ActivityHomeBinding, HomeViewModel>(), TxListRouter {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var giphy: GiphyEcosystem

    private val navController by lazy { Navigation.findNavController(this, R.id.nav_host_fragment) }

    private var appBarConfiguration = AppBarConfiguration(setOf(R.id.txListFragment, R.id.ttlStoreFragment, R.id.profileFragment, R.id.settingsFragment))

    private lateinit var serviceConnection: TariWalletServiceConnection
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    ui.root.postDelayed(Constants.UI.mediumDurationMs) {
                        processIntentDeepLink(it.service!!, intent)
                    }
                }
            }.addTo(compositeDisposable)
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
        if (::serviceConnection.isInitialized && serviceConnection.currentState.status == CONNECTED) {
            processIntentDeepLink(serviceConnection.currentState.service!!, intent)
        } else {
            setIntent(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean = NavigationUI.navigateUp(navController, appBarConfiguration)

    private fun setupUi() {
        setupCTAs()
    }

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
        BottomSlideDialog(
            this,
            R.layout.dialog_incompatible_network,
            canceledOnTouchOutside = false
        ).apply {
            findViewById<CustomFontTextView>(R.id.incompatible_network_description_text_view).text = string(R.string.incompatible_network_description)
                .applyFontStyle(
                    this@HomeActivity,
                    CustomFont.AVENIR_LT_STD_MEDIUM,
                    listOf(
                        string(R.string.incompatible_network_description_bold_part_1),
                        string(R.string.incompatible_network_description_bold_part_2)
                    ),
                    CustomFont.AVENIR_LT_STD_BLACK
                )
            findViewById<View>(R.id.incompatible_network_reset_now_button).setOnThrottledClickListener {
                deleteWallet()
                dismiss()
            }
            findViewById<View>(R.id.incompatible_network_reset_later_button).setOnThrottledClickListener { dismiss() }
        }.show()
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

    override fun toTxDetails(tx: Tx) = startActivity(TxDetailsActivity.createIntent(this, tx))

    override fun toTTLStore() {
        ui.bottomNavigationView.selectedItemId = R.id.ttlStoreFragment
    }

    override fun toAllSettings() {
        ui.bottomNavigationView.selectedItemId = R.id.settingsFragment
    }

    fun willNotifyAboutNewTx(): Boolean = ui.bottomNavigationView.selectedItemId == R.id.txListFragment

    private fun processIntentDeepLink(service: TariWalletService, intent: Intent) {
        DeepLink.from(networkRepository, intent.data?.toString().orEmpty())?.let { deepLink ->
            val pubkey = when (deepLink.type) {
                DeepLink.Type.EMOJI_ID -> service.getPublicKeyFromEmojiId(deepLink.identifier)
                DeepLink.Type.PUBLIC_KEY_HEX -> service.getPublicKeyFromHexString(deepLink.identifier)
            }
            pubkey?.let { publicKey -> sendTariToUser(service, publicKey, deepLink.parameters) }
        }
    }

    private fun sendTariToUser(
        service: TariWalletService,
        recipientPublicKey: PublicKey,
        parameters: Map<String, String>
    ) {
        val error = WalletError()
        val contacts = service.getContacts(error)
        val recipientUser = when (error.code) {
            WalletErrorCode.NO_ERROR -> contacts
                .firstOrNull { it.publicKey == recipientPublicKey } ?: User(recipientPublicKey)
            else -> User(recipientPublicKey)
        }
        val intent = Intent(this, SendTariActivity::class.java)
        intent.putExtra("recipientUser", recipientUser as Parcelable)
        parameters[DeepLink.PARAMETER_NOTE]?.let { intent.putExtra(DeepLink.PARAMETER_NOTE, it) }
        parameters[DeepLink.PARAMETER_AMOUNT]?.toDoubleOrNull()
            ?.let { intent.putExtra(DeepLink.PARAMETER_AMOUNT, it) }
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
        compositeDisposable.dispose()
    }

    companion object {
        private const val NO_SMOOTH_SCROLL = false
    }
}
