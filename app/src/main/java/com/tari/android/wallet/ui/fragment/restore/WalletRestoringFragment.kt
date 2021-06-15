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
package com.tari.android.wallet.ui.fragment.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentWalletRestoringBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.yat.YatUser
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.yat.EmojiId
import com.tari.android.wallet.model.yat.EmojiSet
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.extension.appComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yat.android.data.storage.OAuthTokenPair
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class WalletRestoringFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var applicationContext: Context

    @Inject
    lateinit var yatEmojiSet: EmojiSet

    @Inject
    lateinit var yatUserStorage: YatUserStorage

    @Inject
    lateinit var yatAdapter: YatAdapter

    private lateinit var ui: FragmentWalletRestoringBinding

    private var yatDataIsRestored = AtomicBoolean(false)
    private var continueIsPendingOnYatDataRestoration = AtomicBoolean(false)

    private lateinit var serviceConnection: TariWalletServiceConnection
    private val walletService: TariWalletService?
        get() = serviceConnection.currentState.service

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentWalletRestoringBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No-op
            }
        })
        EventBus.subscribeToWalletState(this, this::onWalletStateChanged)
        WalletService.start(applicationContext)
        bindWalletService()
        ui.root.postDelayed(screenShowDurationMs) {
            if (!yatDataIsRestored.get()) {
                continueIsPendingOnYatDataRestoration.set(true)
            } else {
                (requireActivity() as WalletRestoreRouter).onRestoreCompleted()
            }
        }
    }

    private fun bindWalletService() {
        serviceConnection = ViewModelProvider(
            this,
            TariWalletServiceConnection.TariWalletServiceConnectionFactory(requireActivity())
        ).get()
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING) {
            lifecycleScope.launch(Dispatchers.IO) {
                // restore data after a delay - otherwise the wallet returns an error
                delay(yatDataRestorationDelay)
                restoreYatDataFromWalletDatabase()
                yatDataIsRestored.set(true)
                if (continueIsPendingOnYatDataRestoration.get()) {
                    withContext(Dispatchers.Main) {
                        (requireActivity() as WalletRestoreRouter).onRestoreCompleted()
                    }
                }
            }
        }
    }

    private fun restoreYatDataFromWalletDatabase() {
        val service = walletService ?: return
        val error = WalletError()
        val yatFromWalletDB = EmojiId.of(
            service.getKeyValue(
                WalletService.Companion.KeyValueStorageKeys.YAT_EMOJI_ID,
                error
            ) ?: "",
            yatEmojiSet
        )
        val yatUser = yatUserStorage.get()
        if (yatFromWalletDB != null && yatUser == null) { // after restore
            val restoredAlternateId = service.getKeyValue(
                WalletService.Companion.KeyValueStorageKeys.YAT_USER_ALTERNATE_ID,
                error
            )
            val restoredPassword = service.getKeyValue(
                WalletService.Companion.KeyValueStorageKeys.YAT_USER_PASSWORD,
                error
            )
            val restoredYatUser = YatUser(
                restoredAlternateId,
                restoredPassword,
                setOf(yatFromWalletDB)
            )
            yatUserStorage.put(restoredYatUser)
            val restoredAccessToken = service.getKeyValue(
                WalletService.Companion.KeyValueStorageKeys.YAT_ACCESS_TOKEN,
                error
            )
            val restoredRefreshToken = service.getKeyValue(
                WalletService.Companion.KeyValueStorageKeys.YAT_REFRESH_TOKEN,
                error
            )
            val tokenPair = OAuthTokenPair(restoredAccessToken, restoredRefreshToken)
            yatAdapter.getJWTStorage().put(tokenPair)
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = WalletRestoringFragment()

        private const val yatDataRestorationDelay = 2000L
        private const val screenShowDurationMs = 5000L
    }

}
