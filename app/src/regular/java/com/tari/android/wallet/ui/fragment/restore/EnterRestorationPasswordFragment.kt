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
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.DriveScopes
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentEnterRestorePasswordBinding
import com.tari.android.wallet.infrastructure.backup.WalletRestoration
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

class EnterRestorationPasswordFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    private lateinit var ui: FragmentEnterRestorePasswordBinding
    private lateinit var viewModel: RestorationWithCloudViewModel
    private val blockingBackPressDispatcher = object : OnBackPressedCallback(false) {
        // No-p by design
        override fun handleOnBackPressed() = Unit
    }

    // Needed to carry the password over the onActivityResult phase
    // TODO(nyarian): implement using androidx ActivityResultCallback? https://developer.android.com/training/basics/intents/result
    private val restorationPassword = AtomicReference<CharArray>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentEnterRestorePasswordBinding.inflate(inflater, container, false)
            .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        viewModel = ViewModelProvider(requireActivity()).get()
    }

    private fun setupUi() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
        setPageDescription()
        ui.passwordEditText.requestFocus()
        UiUtil.showKeyboard(requireActivity())
        ui.restoringProgressBar.setColor(color(white))
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.restoreWalletCtaView.setOnClickListener {
            showRestoringUi()
            performRestoration(ui.passwordEditText.text!!
                .run { CharArray(length).also { getChars(0, length, it, 0) } })
        }
        ui.passwordEditText.addTextChangedListener(
            afterTextChanged = {
                ui.enterPasswordLabelTextView.setTextColor(color(black))
                ui.passwordEditText.setTextColor(color(black))
                ui.wrongPasswordLabelView.gone()
            }
        )
    }

    private fun showRestoringUi() {
        blockingBackPressDispatcher.isEnabled = true
        ui.passwordEditText.isEnabled = false
        ui.restoreWalletCtaView.isClickable = false
        ui.restoreWalletTextView.gone()
        ui.restoringProgressBar.visible()
        UiUtil.hideKeyboard(requireActivity())
    }

    private fun showInputUi() {
        blockingBackPressDispatcher.isEnabled = false
        ui.passwordEditText.isEnabled = true
        ui.restoreWalletCtaView.isClickable = true
        ui.restoreWalletTextView.visible()
        ui.restoringProgressBar.gone()
        ui.passwordEditText.requestFocus()
        UiUtil.showKeyboard(requireActivity())
    }

    private fun showWrongPasswordErrorLabels() {
        ui.enterPasswordLabelTextView.setTextColor(color(common_error))
        ui.passwordEditText.setTextColor(color(common_error))
        ui.wrongPasswordLabelView.visible()
    }

    private fun setPageDescription() {
        val generalPart = string(enter_backup_password_page_desc_general_part)
        val highlightedPart =
            SpannableString(string(enter_backup_password_page_desc_highlighted_part))
        val spanColor = ForegroundColorSpan(color(black))
        highlightedPart.setSpan(spanColor, 0, highlightedPart.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        ui.pageDescriptionTextView.text = SpannableStringBuilder().apply {
            insert(0, generalPart)
            insert(generalPart.length, " ")
            insert(generalPart.length + 1, highlightedPart)
            insert(generalPart.length + highlightedPart.length + 1, ".")
        }
    }

    private fun performRestoration(password: CharArray) {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            if (it.status == RestorationStatus.FAILURE) {
                handleRestorationFailure(it.exception!!.cause ?: it.exception, password)
                viewModel.state.removeObservers(viewLifecycleOwner)
                viewModel.reset()
            } else if (it.status == RestorationStatus.SUCCESS) {
                signOutFromGDrive()
                (requireActivity() as WalletRestoreRouter).toRestorationWithCloud()
                viewModel.state.removeObservers(viewLifecycleOwner)
            }
        })
        viewModel.restoreWallet(password)
    }

    private fun signOutFromGDrive() {
        GoogleSignIn.getClient(
            requireActivity(), GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        ).signOut()
    }

    private fun handleRestorationFailure(e: Throwable, password: CharArray) = when (e) {
        is UserRecoverableAuthIOException -> {
            restorationPassword.set(password)
            startActivityForResult(e.intent, REQUEST_CODE_REAUTH)
        }
        is BadPaddingException, is IllegalBlockSizeException -> {
            showWrongPasswordErrorLabels()
            showInputUi()
        }
        else -> showUnrecoverableExceptionDialog(deductUnrecoverableErrorMessage(e))
    }

    private fun deductUnrecoverableErrorMessage(throwable: Throwable): String = when {
        throwable is UnknownHostException ->
            string(error_no_connection_title)
        throwable.message != null -> throwable.message!!
        else -> string(common_unknown_error)
    }

    private fun showUnrecoverableExceptionDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(restore_wallet_with_cloud_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = {
                blockingBackPressDispatcher.isEnabled = false
                requireActivity().onBackPressed()
                viewModel.reset()
            }
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_REAUTH) {
            if (resultCode == Activity.RESULT_OK) {
                performRestoration(restorationPassword.get())
                restorationPassword.set(null)
            } else {
                showUnrecoverableExceptionDialog(
                    string(back_up_wallet_status_check_authentication_cancellation)
                )
            }
        }
        restorationPassword.set(null)
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = EnterRestorationPasswordFragment()

        private const val REQUEST_CODE_REAUTH = 11222
    }

    class RestorationWithCloudViewModel(private val restoration: WalletRestoration) : ViewModel() {

        private val _state = MutableLiveData<RestorationState>()
        val state: LiveData<RestorationState> get() = _state
        private val currentState get() = _state.value!!

        init {
            propagateIdleState()
        }

        fun restoreWallet(password: CharArray) {
            // AtomicReference to achieve safe publication
            val passwordReference = AtomicReference<CharArray>(password)
            if (currentState.status == RestorationStatus.IDLE) {
                _state.value = RestorationState(RestorationStatus.PROCESSING)
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) { restoration.run(passwordReference.get()) }
                        _state.value = RestorationState(RestorationStatus.SUCCESS)
                    } catch (e: Exception) {
                        Logger.e(e, "Exception occurred during restoration: $e")
                        _state.value = RestorationState(RestorationStatus.FAILURE, e)
                    }
                    Arrays.fill(passwordReference.get(), 0x00.toChar())
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
            require(modelClass === RestorationWithCloudViewModel::class.java)
            return RestorationWithCloudViewModel(restoration) as T
        }

    }

}
