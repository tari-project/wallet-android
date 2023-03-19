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
package com.tari.android.wallet.ui.fragment.settings.backup.changeSecurePassword

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R.string.back_up_wallet_backing_up_error_desc
import com.tari.android.wallet.R.string.back_up_wallet_backing_up_error_title
import com.tari.android.wallet.R.string.back_up_wallet_backing_up_unknown_error
import com.tari.android.wallet.R.string.change_password_change_password_cta
import com.tari.android.wallet.R.string.change_password_page_description_general_part
import com.tari.android.wallet.R.string.change_password_page_description_highlight_part
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.databinding.FragmentChangeSecurePasswordBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupState.BackupFailed
import com.tari.android.wallet.infrastructure.backup.BackupState.BackupUpToDate
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.extension.animateClick
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.scrollToBottom
import com.tari.android.wallet.ui.extension.scrollToTop
import com.tari.android.wallet.ui.extension.showKeyboard
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException


class ChangeSecurePasswordFragment : CommonFragment<FragmentChangeSecurePasswordBinding, ChangeSecurePasswordViewModel>() {

    private lateinit var inputService: InputMethodManager

    private val passwordInput
        get() = ui.enterPasswordEditText.ui.editText

    private val confirmInput
        get() = ui.confirmPasswordEditText.ui.editText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChangeSecurePasswordBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChangeSecurePasswordViewModel by viewModels()
        bindViewModel(viewModel)

        inputService = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        setupViews()
    }

    override fun onDestroyView() {
        EventBus.backupState.unsubscribe(this)
        super.onDestroyView()
    }

    private fun setupViews() {
        passwordInput.isFocusableInTouchMode = true
        ui.performingBackupProgressBar.setWhite()
        ui.contentContainerView.setOnClickListener { clearPasswordFieldsFocusAndHideKeyboard(it) }
        setPageDescription()
        setValidatingTextFieldsListeners()
        setCTAs()
        promptPasswordEnter()
    }

    private fun clearPasswordFieldsFocusAndHideKeyboard(focusedView: View) {
        passwordInput.clearFocus()
        confirmInput.clearFocus()
        inputService.hideSoftInputFromWindow(focusedView.windowToken, 0)
        ui.contentScrollView.scrollToTop()
    }

    private fun promptPasswordEnter() {
        passwordInput.postDelayed({
            passwordInput.requestFocus()
            requireActivity().showKeyboard()
            passwordInput.postDelayed(KEYBOARD_ANIMATION_TIME) { ui.contentScrollView.scrollToBottom() }
        }, KEYBOARD_SHOW_UP_DELAY_AFTER_LOCAL_AUTH)
    }

    private fun setPageDescription() {
        val generalPart = string(change_password_page_description_general_part)
        val highlightedPart = SpannableString(string(change_password_page_description_highlight_part))
        val spanColor = ForegroundColorSpan(viewModel.paletteManager.getTextHeading(requireContext()))
        highlightedPart.setSpan(spanColor, 0, highlightedPart.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        ui.pageDescriptionTextView.text = SpannableStringBuilder().apply {
            insert(0, generalPart)
            insert(generalPart.length, " ")
            insert(generalPart.length + 1, highlightedPart)
            insert(generalPart.length + highlightedPart.length + 1, ".")
        }
    }

    private fun setValidatingTextFieldsListeners() {
        passwordInput.addTextChangedListener(afterTextChanged = {
            updateVerifyButtonStateBasedOnEditTexts(
                it,
                !passwordIsLongEnough() || (!confirmInput.text.isNullOrEmpty() && !doPasswordsMatch())
            )
        })
        confirmInput.addTextChangedListener(afterTextChanged = {
            updateVerifyButtonStateBasedOnEditTexts(
                it,
                !passwordIsLongEnough() || (!passwordInput.text.isNullOrEmpty() && !doPasswordsMatch())
            )
        })
        val passwordTextFieldListener = View.OnFocusChangeListener { _, _ ->
            // password
            if (!passwordInput.hasFocus() && !passwordIsLongEnough()) {
                setPasswordTooShortErrorState()
            } else {
                setPlainInputState(ui.passwordTooShortLabelView, listOf(passwordInput, ui.enterPasswordLabelTextView))
            }
            // confirm
            if (!confirmInput.hasFocus() && passwordIsLongEnough() && !doPasswordsMatch()) {
                setPasswordMatchErrorState()
            } else {
                setPlainInputState(ui.passwordsNotMatchLabelView, listOf(confirmInput, ui.confirmPasswordLabelTextView))
            }
        }
        passwordInput.onFocusChangeListener = passwordTextFieldListener
        confirmInput.onFocusChangeListener = passwordTextFieldListener
        confirmInput.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                clearPasswordFieldsFocusAndHideKeyboard(v)
            }
            false
        }
    }

    private fun updateVerifyButtonStateBasedOnEditTexts(trigger: Editable?, errorCondition: Boolean) {
        val passwordIsLongEnough = passwordIsLongEnough()
        val passwordsMatch = doPasswordsMatch()
        if (passwordsMatch || trigger.isNullOrEmpty()) {
            val canChangePassword = areTextFieldsFilled() && passwordIsLongEnough && passwordsMatch
            setVerifyButtonState(isEnabled = canChangePassword)
            if (canChangePassword) {
                setPlainInputState(ui.passwordTooShortLabelView, listOf(passwordInput, ui.enterPasswordLabelTextView))
                setPlainInputState(ui.passwordsNotMatchLabelView, listOf(confirmInput, ui.confirmPasswordLabelTextView))
            }
        } else if (errorCondition) {
            setVerifyButtonState(isEnabled = false)
        }
    }

    private fun areTextFieldsFilled() = passwordInput.text!!.isNotEmpty() && confirmInput.text!!.isNotEmpty()

    private fun passwordIsLongEnough() = (passwordInput.text?.toString() ?: "").length >= 6

    private fun doPasswordsMatch() = confirmInput.text?.toString() == passwordInput.text?.toString()

    private fun setPlainInputState(errorLabel: TextView, inputTextViews: Iterable<TextView>) {
        errorLabel.gone()
        inputTextViews.forEach { it.setTextColor(viewModel.paletteManager.getTextHeading(requireContext())) }
    }

    private fun setPasswordTooShortErrorState() {
        ui.passwordTooShortLabelView.visible()
        ui.enterPasswordLabelTextView.setTextColor(viewModel.paletteManager.getRed(requireContext()))
        passwordInput.setTextColor(viewModel.paletteManager.getRed(requireContext()))
    }

    private fun setPasswordMatchErrorState() {
        ui.passwordsNotMatchLabelView.visible()
        ui.confirmPasswordLabelTextView.setTextColor(viewModel.paletteManager.getRed(requireContext()))
        confirmInput.setTextColor(viewModel.paletteManager.getRed(requireContext()))
    }

    private fun setVerifyButtonState(isEnabled: Boolean) {
        ui.setPasswordCtaTextView.isEnabled = isEnabled
    }

    private fun setCTAs() {
        ui.setPasswordCtaTextView.setOnClickListener {
            ui.setPasswordCtaContainerView.animateClick()
            requireActivity().hideKeyboard()
            preventExitAndPasswordEditing()
            setSecurePasswordCtaClickedState()
            performBackupAndUpdatePassword()
        }
    }

    private fun preventExitAndPasswordEditing() {
        passwordInput.isEnabled = false
        confirmInput.isEnabled = false
    }

    private fun allowExitAndPasswordEditing() {
        passwordInput.isEnabled = true
        confirmInput.isEnabled = true
    }

    private fun setSecurePasswordCtaClickedState() {
        ui.performingBackupProgressBar.visible()
        ui.setPasswordCtaTextView.isClickable = false
        ui.setPasswordCtaTextView.text = ""
    }

    private fun setSecurePasswordCtaIdleState() {
        ui.performingBackupProgressBar.gone()
        ui.setPasswordCtaTextView.isClickable = true
        ui.setPasswordCtaTextView.text = string(change_password_change_password_cta)
    }

    private fun performBackupAndUpdatePassword() {
        // start listening to wallet events
        subscribeToBackupState()
        viewModel.backupSharedPrefsRepository.backupPassword = passwordInput.text!!.toString()
        viewModel.backupManager.backupNow()
    }

    private fun subscribeToBackupState() {
        EventBus.backupState.subscribe(this) { backupState ->
            lifecycleScope.launch(Dispatchers.Main) {
                onBackupStateChanged(backupState.backupsState)
            }
        }
    }

    private fun onBackupStateChanged(backupState: BackupState?) {
        when (backupState) {
            is BackupUpToDate -> {
                allowExitAndPasswordEditing()
                HomeActivity.instance.get()?.tariNavigator?.onPasswordChanged()
            }
            is BackupFailed -> { // backup failed
                showBackupErrorDialog(deductBackupErrorMessage(backupState.backupException)) {
                    allowExitAndPasswordEditing()
                    setSecurePasswordCtaIdleState()
                }
            }
            else -> Unit
        }
    }

    private fun deductBackupErrorMessage(e: Throwable?): String = when {
        e is UnknownHostException -> string(error_no_connection_title)
        e?.message != null -> string(back_up_wallet_backing_up_error_desc, e.message!!)
        else -> string(back_up_wallet_backing_up_unknown_error)
    }

    private fun showBackupErrorDialog(message: String, onClose: () -> Unit) {
        val args = ErrorDialogArgs(
            string(back_up_wallet_backing_up_error_title), message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = onClose
        )
        ModularDialog(requireContext(), args.getModular(viewModel.resourceManager)).show()
    }

    companion object {
        private const val KEYBOARD_SHOW_UP_DELAY_AFTER_LOCAL_AUTH = 500L
        private const val KEYBOARD_ANIMATION_TIME = 100L
    }

}
