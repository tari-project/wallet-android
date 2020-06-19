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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentWalletRestoringBinding
import com.tari.android.wallet.infrastructure.backup.WalletRestoration
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestorationWithCloudFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    private lateinit var ui: FragmentWalletRestoringBinding
    private lateinit var state: RestorationWithCloudState
    private lateinit var blockingBackPressDispatcher: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentWalletRestoringBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blockingBackPressDispatcher = object : OnBackPressedCallback(true) {
            // No-p by design
            override fun handleOnBackPressed() = Unit
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
        state = ViewModelProvider(requireActivity()).get()
        state.state.observe(viewLifecycleOwner, Observer {
            if (it.status == RestorationStatus.FAILURE) {
                handleRestorationException(it)
            } else if (it.status == RestorationStatus.SUCCESS) {
                (requireActivity() as WalletRestoreRouter).onBackupCompleted()
            }
        })
    }

    private fun handleRestorationException(state: RestorationState) {
        val exception = state.exception!!
        if (exception is UserRecoverableAuthIOException) {
            startActivityForResult(exception.intent, REQUEST_CODE_REAUTH)
        } else {
            showUnrecoverableExceptionDialog(
                exception.message ?: string(
                    back_up_wallet_status_check_unknown_error
                )
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_REAUTH) {
            if (resultCode == Activity.RESULT_OK) {
                state.reset()
                state.restoreWallet()
            } else {
                showUnrecoverableExceptionDialog(
                    string(back_up_wallet_status_check_authentication_cancellation)
                )
            }
        }
    }


    private fun showUnrecoverableExceptionDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(restore_wallet_with_cloud_error_title),
            description = string(restore_wallet_with_cloud_error_desc, message),
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = {
                blockingBackPressDispatcher.isEnabled = false
                requireActivity().onBackPressed()
                this.state.reset()
            }
        ).show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = RestorationWithCloudFragment()

        private const val REQUEST_CODE_REAUTH = 11222
    }

    class RestorationWithCloudState(private val restoration: WalletRestoration) : ViewModel() {

        private val _state = MutableLiveData<RestorationState>()
        val state: LiveData<RestorationState> get() = _state
        private val currentState get() = _state.value!!

        init {
            propagateIdleState()
        }

        fun restoreWallet() {
            if (currentState.status == RestorationStatus.IDLE) {
                _state.value = RestorationState(RestorationStatus.PROCESSING)
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) { restoration.run() }
                        _state.value = RestorationState(RestorationStatus.SUCCESS)
                    } catch (e: Exception) {
                        Logger.e(e, "Exception occurred during restoration")
                        _state.value = RestorationState(RestorationStatus.FAILURE, e)
                    }
                }
            }
        }

        fun reset() {
            if (currentState.status != RestorationStatus.PROCESSING) {
                propagateIdleState()
            }
        }

        private fun propagateIdleState() {
            _state.value = RestorationState(RestorationStatus.IDLE)
        }

    }

    enum class RestorationStatus { IDLE, PROCESSING, FAILURE, SUCCESS }

    data class RestorationState(val status: RestorationStatus, val exception: Exception?) {
        constructor(status: RestorationStatus) : this(status, null)
    }

    class RestorationWithCloudStateFactory(private val restoration: WalletRestoration) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(modelClass === RestorationWithCloudState::class.java)
            return RestorationWithCloudState(restoration) as T
        }

    }

}
