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
package com.tari.android.wallet.ui.screen.home

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.INDEX_CHAT
import com.tari.android.wallet.navigation.TariNavigator.Companion.INDEX_CONTACT_BOOK
import com.tari.android.wallet.navigation.TariNavigator.Companion.INDEX_HOME
import com.tari.android.wallet.navigation.TariNavigator.Companion.INDEX_SETTINGS
import com.tari.android.wallet.navigation.TariNavigator.Companion.NO_SMOOTH_SCROLL
import com.tari.android.wallet.notification.TariFirebaseMessagingService
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.screen.StartActivity
import com.tari.android.wallet.ui.screen.chat.chatList.ChatListFragment
import com.tari.android.wallet.ui.screen.contactBook.root.ContactBookFragment
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewFragment
import com.tari.android.wallet.ui.screen.settings.allSettings.AllSettingsFragment
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.store.StoreFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.serializable
import com.tari.android.wallet.util.extension.setVisible
import java.lang.ref.WeakReference
import javax.inject.Inject

class HomeActivity : CommonActivity<ActivityHomeBinding, HomeViewModel>() {

    @Inject
    lateinit var securityPrefRepository: SecurityPrefRepository

    @Inject
    lateinit var networkRepository: NetworkPrefRepository

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var tariSettingsRepository: TariSettingsPrefRepository

    // TODO check GP services installed GoogleApiAvailability.makeGooglePlayServicesAvailable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        onBackPressedDispatcher.addCallback {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStackImmediate()
            } else {
                if (ui.viewPager.currentItem == INDEX_HOME) {
                    finish()
                } else {
                    ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
                    enableNavigationView(ui.homeImageView)
                }
            }
        }

        instance = WeakReference(this)

        val viewModel: HomeViewModel by viewModels()
        bindViewModel(viewModel)

        subscribeToCommon(viewModel.shareViewModel)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothServer)
        subscribeToCommon(viewModel.shareViewModel.tariBluetoothClient)

        viewModel.shareViewModel.tariBluetoothServer.init(this)
        viewModel.shareViewModel.tariBluetoothClient.init(this)

        setContainerId(R.id.nav_container)

        if (!securityPrefRepository.isAuthenticated) {
            val intent = Intent(this, StartActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            this.intent?.data?.let(intent::setData)
            finish()
            startActivity(intent)
            return
        }
        ui = ActivityHomeBinding.inflate(layoutInflater).also { setContentView(it.root) }

        val buttonBg = when (tariSettingsRepository.currentTheme) {
            TariTheme.AppBased -> {
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> R.drawable.vector_disable_able_gradient_button_bg_external_dark
                    else -> R.drawable.vector_disable_able_gradient_button_bg_external
                }
            }

            TariTheme.Light -> R.drawable.vector_disable_able_gradient_button_bg_external

            else -> R.drawable.vector_disable_able_gradient_button_bg_external_dark
        }
        ui.sendButtonExternalContainer.setBackgroundResource(buttonBg)

        if (savedInstanceState == null) {
            enableNavigationView(ui.homeImageView)
            viewModel.doOnWalletRunning {
                ui.root.postDelayed({ processIntentDeepLink(intent) }, Constants.UI.mediumDurationMs)
            }
        } else {
            val index = savedInstanceState.getInt(KEY_PAGE)
            ui.viewPager.setCurrentItem(index, false)
            enableNavigationView(index)
        }
        setupUi()
        subscribeUI()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(POST_NOTIFICATIONS), 0)
        }

        pushTest()
    }

    // TODO Remove after testing!!
    private fun pushTest() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Logger.t(TariFirebaseMessagingService::class.simpleName).d("Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = "FCM registration token: $token"
            Logger.t(TariFirebaseMessagingService::class.simpleName).d(msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.securityPrefRepository.isAuthenticated) {
            viewModel.navigateToAuth(this.intent.data)
            finish()
        }
    }

    private fun subscribeUI() = with(viewModel) {
        observe(shareViewModel.shareText) { shareViaText(it) }
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
        checkScreensDeeplink(intent)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, ui.viewPager.currentItem)
    }

    fun setBottomBarVisibility(isVisible: Boolean) {
        val postDelay = if (!isVisible) 0 else Constants.UI.shortDurationMs
        ui.bottomNavigationView.postDelayed({
            ui.bottomNavigationView.setVisible(isVisible)
            ui.sendTariButton.setVisible(isVisible)
        }, postDelay)
    }

    private fun setupUi() {
        ui.sendTariButton.setOnClickListener { viewModel.tariNavigator.navigate(Navigation.TxList.ToTransfer) }
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        enableNavigationView(ui.homeImageView)
        ui.viewPager.adapter = getBottomMenuAdapter()
        ui.viewPager.isUserInputEnabled = false
        ui.viewPager.offscreenPageLimit = 3
        ui.homeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_HOME, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.homeImageView)
        }
        ui.storeImageView.setImageResource(if (DebugConfig.isChatEnabled) R.drawable.vector_home_book else R.drawable.vector_ttl_store_icon)
        ui.storeView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_CONTACT_BOOK, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.storeImageView)
        }
        ui.chatImageView.setImageResource(if (DebugConfig.isChatEnabled) R.drawable.vector_home_chat else R.drawable.vector_home_book)
        ui.chatView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_CHAT, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.chatImageView)
        }
        ui.settingsView.setOnClickListener {
            ui.viewPager.setCurrentItem(INDEX_SETTINGS, NO_SMOOTH_SCROLL)
            enableNavigationView(ui.settingsImageView)
        }
    }

    private fun enableNavigationView(index: Int) {
        val view: ImageView = when (index) {
            INDEX_HOME -> ui.homeImageView
            INDEX_CHAT -> ui.storeImageView
            INDEX_CONTACT_BOOK -> ui.chatImageView
            INDEX_SETTINGS -> ui.settingsImageView
            else -> error("Unexpected index: $index")
        }
        enableNavigationView(view)
    }

    private fun enableNavigationView(view: ImageView) {
        arrayOf(ui.homeImageView, ui.storeImageView, ui.chatImageView, ui.settingsImageView).forEach { it.clearColorFilter() }
        view.setColorFilter(PaletteManager.getTextHeading(this))
    }

    private fun checkScreensDeeplink(intent: Intent) {
        val screen = intent.getStringExtra(HomeDeeplinkScreens.KEY)
        if (screen.orEmpty().isNotEmpty()) {
            when (HomeDeeplinkScreens.parse(screen)) {
                HomeDeeplinkScreens.TxDetails -> {
                    (intent.serializable<TxId>(HomeDeeplinkScreens.KEY_TX_DETAIL_ARGS))
                        ?.let { viewModel.tariNavigator.navigate(Navigation.TxList.ToTxDetails(txId = it)) }
                }

                else -> {}
            }
        }
    }

    fun willNotifyAboutNewTx(): Boolean = ui.viewPager.currentItem == INDEX_HOME

    private fun processIntentDeepLink(intent: Intent) {
        intent.data?.toString()?.takeIf { it.isNotEmpty() }
            ?.let { deeplinkString -> deeplinkManager.parseDeepLink(deeplinkString) }
            ?.let { deeplink ->
                deeplinkManager.execute(context = this, deeplink = deeplink)
            }
    }

    override fun onDestroy() {
        viewModel.securityPrefRepository.isAuthenticated = false
        super.onDestroy()
        instance = WeakReference(null)
        viewModelStore.clear()
//        ui.viewPager.adapter = null
    }

    private fun getBottomMenuAdapter(): FragmentStateAdapter = if (DebugConfig.isChatEnabled) {
        HomeChatAdapter(supportFragmentManager, this.lifecycle)
    } else {
        HomeStoreAdapter(supportFragmentManager, this.lifecycle)
    }

    private class HomeStoreAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

        override fun createFragment(position: Int): Fragment = when (position) {
            INDEX_HOME -> HomeOverviewFragment()
            INDEX_CONTACT_BOOK -> StoreFragment.newInstance()
            INDEX_CHAT -> ContactBookFragment()
            INDEX_SETTINGS -> AllSettingsFragment.newInstance()
            else -> error("Unexpected position: $position")
        }

        override fun getItemCount(): Int = 4
    }

    private class HomeChatAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

        override fun createFragment(position: Int): Fragment = when (position) {
            INDEX_HOME -> HomeOverviewFragment()
            INDEX_CONTACT_BOOK -> ContactBookFragment()
            INDEX_CHAT -> ChatListFragment()
            INDEX_SETTINGS -> AllSettingsFragment.newInstance()
            else -> error("Unexpected position: $position")
        }

        override fun getItemCount(): Int = 4
    }

    companion object {

        private const val KEY_PAGE = "key_page"

        @Volatile
        var instance: WeakReference<HomeActivity> = WeakReference(null) // TODO I don't like this. Better not to share context globally
            private set
    }
}
