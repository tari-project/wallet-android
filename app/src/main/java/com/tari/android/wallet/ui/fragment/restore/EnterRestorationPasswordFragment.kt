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
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentEnterRestorePasswordBinding
import com.tari.android.wallet.infrastructure.backup.BackupStorage
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UIUtil
import com.tari.android.wallet.ui.util.UIUtil.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import javax.inject.Inject

class EnterRestorationPasswordFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var backupStorage: BackupStorage

    private lateinit var ui: FragmentEnterRestorePasswordBinding
    private val blockingBackPressDispatcher = object : OnBackPressedCallback(false) {
        // No-op by design
        override fun handleOnBackPressed() = Unit
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentEnterRestorePasswordBinding.inflate(inflater, container, false)
            .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
        setPageDescription()
        ui.passwordEditText.requestFocus()
        UIUtil.showKeyboard(requireActivity())
        ui.restoringProgressBar.setColor(color(white))
        ui.backCtaView.setOnClickListener(
            ThrottleClick {
                requireActivity().onBackPressed()
                lifecycleScope.launch(Dispatchers.IO) {
                    backupStorage.signOut()
                }
            }
        )
        setRestoreWalletCTAState(isEnabled = false)
        ui.restoreWalletCtaView.setOnClickListener {
            showRestoringUI()
            performRestoration(ui.passwordEditText.text!!.toString())
        }
        ui.passwordEditText.addTextChangedListener(
            afterTextChanged = {
                setRestoreWalletCTAState(it?.length ?: 0 != 0)
                ui.enterPasswordLabelTextView.setTextColor(color(black))
                ui.passwordEditText.setTextColor(color(black))
                ui.wrongPasswordLabelView.gone()
            }
        )
    }

    private fun setRestoreWalletCTAState(isEnabled: Boolean) {
        if (ui.restoreWalletCtaView.isEnabled == isEnabled) return
        ui.restoreWalletCtaView.isEnabled = isEnabled
        ui.restoreWalletTextView.setTextColor(
            if (isEnabled) Color.WHITE
            else color(seed_phrase_button_disabled_text_color)
        )
    }

    private fun showRestoringUI() {
        blockingBackPressDispatcher.isEnabled = true
        ui.passwordEditText.isEnabled = false
        ui.restoreWalletCtaView.isClickable = false
        ui.restoreWalletTextView.gone()
        ui.restoringProgressBar.visible()
        UIUtil.hideKeyboard(requireActivity())
    }

    private fun showInputUI() {
        blockingBackPressDispatcher.isEnabled = false
        ui.passwordEditText.isEnabled = true
        ui.restoreWalletCtaView.isClickable = true
        ui.restoreWalletTextView.visible()
        ui.restoringProgressBar.gone()
        ui.passwordEditText.requestFocus()
        UIUtil.showKeyboard(requireActivity())
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

    private fun performRestoration(password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backupStorage.restoreLatestBackup(password)
                (requireActivity() as WalletRestoreRouter).toRestoreInProgress()
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    handleRestorationFailure(exception)
                }
            }
        }
    }

    private fun handleRestorationFailure(exception: Exception) {
        exception.cause?.let {
            if (it is GeneralSecurityException) {
                showWrongPasswordErrorLabels()
                showInputUI()
                return
            }
        }
        when (exception) {
            is GeneralSecurityException -> {
                showWrongPasswordErrorLabels()
                showInputUI()
            }
            else -> showUnrecoverableExceptionDialog(deductUnrecoverableErrorMessage(exception))
        }
    }

    private fun deductUnrecoverableErrorMessage(throwable: Throwable): String = when {
        throwable.message != null -> throwable.message!!
        else -> string(common_unknown_error)
    }

    private fun showUnrecoverableExceptionDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(restore_wallet_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = {
                blockingBackPressDispatcher.isEnabled = false
                requireActivity().onBackPressed()
            }
        ).show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = EnterRestorationPasswordFragment()
    }

}
