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
package com.tari.android.wallet.ui.fragment.settings.backup

import android.content.Context
import android.graphics.Color
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentChangeSecurePasswordBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.inject.Inject
import com.tari.android.wallet.infrastructure.backup.BackupState.*

internal class ChangeSecurePasswordFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsRepository

    @Inject
    lateinit var backupManager: BackupManager

    private lateinit var ui: FragmentChangeSecurePasswordBinding
    private lateinit var inputService: InputMethodManager

    private var subscribedToBackupState = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inputService =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentChangeSecurePasswordBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onDestroyView() {
        EventBus.backupState.unsubscribe(this)
        super.onDestroyView()
    }

    private fun setupViews() {
        ui.enterPasswordEditText.isFocusableInTouchMode = true
        ui.performingBackupProgressBar.setColor(color(white))
        ui.contentContainerView.setOnClickListener { clearPasswordFieldsFocusAndHideKeyboard(it) }
        setPageDescription()
        setValidatingTextFieldsListeners()
        setCTAs()
        promptPasswordEnter()
    }

    private fun clearPasswordFieldsFocusAndHideKeyboard(focusedView: View) {
        ui.enterPasswordEditText.clearFocus()
        ui.confirmPasswordEditText.clearFocus()
        inputService.hideSoftInputFromWindow(focusedView.windowToken, 0)
        ui.contentScrollView.scrollToTop()
    }

    private fun promptPasswordEnter() {
        ui.enterPasswordEditText.postDelayed({
            ui.enterPasswordEditText.requestFocus()
            requireActivity().showKeyboard()
            ui.enterPasswordEditText
                .postDelayed(KEYBOARD_ANIMATION_TIME) { ui.contentScrollView.scrollToBottom() }
        }, KEYBOARD_SHOWUP_DELAY_AFTER_LOCAL_AUTH)
    }

    private fun setPageDescription() {
        val generalPart = string(change_password_page_description_general_part)
        val highlightedPart =
            SpannableString(string(change_password_page_description_highlight_part))
        val spanColor = ForegroundColorSpan(color(black))
        highlightedPart.setSpan(spanColor, 0, highlightedPart.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        ui.pageDescriptionTextView.text = SpannableStringBuilder().apply {
            insert(0, generalPart)
            insert(generalPart.length, " ")
            insert(generalPart.length + 1, highlightedPart)
            insert(generalPart.length + highlightedPart.length + 1, ".")
        }
    }

    private fun setValidatingTextFieldsListeners() {
        ui.enterPasswordEditText.addTextChangedListener(afterTextChanged = {
            updateVerifyButtonStateBasedOnEditTexts(
                it,
                !passwordIsLongEnough()
                        || (!ui.confirmPasswordEditText.text.isNullOrEmpty() && !doPasswordsMatch())
            )
        }
        )
        ui.confirmPasswordEditText.addTextChangedListener(afterTextChanged = {
            updateVerifyButtonStateBasedOnEditTexts(
                it,
                !passwordIsLongEnough()
                        || (!ui.confirmPasswordEditText.text.isNullOrEmpty() && !doPasswordsMatch())
            )
        })
        val passwordTextFieldListener = View.OnFocusChangeListener { _, _ ->
            // password
            if (!ui.enterPasswordEditText.hasFocus()
                && !passwordIsLongEnough()
            ) {
                setPasswordTooShortErrorState()
            } else {
                setPlainInputState(
                    ui.passwordTooShortLabelView,
                    listOf(
                        ui.enterPasswordEditText,
                        ui.enterPasswordLabelTextView
                    )
                )
            }
            // confirm
            if (!ui.confirmPasswordEditText.hasFocus()
                && passwordIsLongEnough()
                && !doPasswordsMatch()
            ) {
                setPasswordMatchErrorState()
            } else {
                setPlainInputState(
                    ui.passwordsNotMatchLabelView,
                    listOf(
                        ui.confirmPasswordEditText,
                        ui.confirmPasswordLabelTextView
                    )
                )
            }
        }
        ui.enterPasswordEditText.onFocusChangeListener = passwordTextFieldListener
        ui.confirmPasswordEditText.onFocusChangeListener = passwordTextFieldListener
        ui.confirmPasswordEditText.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                clearPasswordFieldsFocusAndHideKeyboard(v)
            }
            false
        }
    }

    private fun updateVerifyButtonStateBasedOnEditTexts(
        trigger: Editable?,
        errorCondition: Boolean
    ) {
        val passwordIsLongEnough = passwordIsLongEnough()
        val passwordsMatch = doPasswordsMatch()
        if (passwordsMatch || trigger.isNullOrEmpty()) {
            val canChangePassword = areTextFieldsFilled() && passwordIsLongEnough && passwordsMatch
            setVerifyButtonState(isEnabled = canChangePassword)
            if (canChangePassword) {
                setPlainInputState(
                    ui.passwordTooShortLabelView,
                    listOf(
                        ui.enterPasswordEditText,
                        ui.enterPasswordLabelTextView
                    )
                )
                setPlainInputState(
                    ui.passwordsNotMatchLabelView,
                    listOf(
                        ui.confirmPasswordEditText,
                        ui.confirmPasswordLabelTextView
                    )
                )
            }
        } else if (errorCondition) {
            setVerifyButtonState(isEnabled = false)
        }
    }

    private fun areTextFieldsFilled() = ui.enterPasswordEditText.text!!.isNotEmpty() &&
            ui.confirmPasswordEditText.text!!.isNotEmpty()

    private fun passwordIsLongEnough() =
        (ui.enterPasswordEditText.text?.toString() ?: "").length >= 6

    private fun doPasswordsMatch() =
        ui.confirmPasswordEditText.text?.toString() == ui.enterPasswordEditText.text?.toString()

    private fun setPlainInputState(errorLabel: TextView, inputTextViews: Iterable<TextView>) {
        errorLabel.gone()
        inputTextViews.forEach { it.setTextColor(color(black)) }
    }

    private fun setPasswordTooShortErrorState() {
        ui.passwordTooShortLabelView.visible()
        ui.enterPasswordLabelTextView.setTextColor(color(common_error))
        ui.enterPasswordEditText.setTextColor(color(common_error))
    }

    private fun setPasswordMatchErrorState() {
        ui.passwordsNotMatchLabelView.visible()
        ui.confirmPasswordLabelTextView.setTextColor(color(common_error))
        ui.confirmPasswordEditText.setTextColor(color(common_error))
    }

    private fun setVerifyButtonState(isEnabled: Boolean) {
        if (ui.setPasswordCtaTextView.isEnabled == isEnabled) return
        ui.setPasswordCtaTextView.isEnabled = isEnabled
        ui.setPasswordCtaTextView.setTextColor(
            if (isEnabled) Color.WHITE
            else color(seed_phrase_button_disabled_text_color)
        )
    }

    private fun setCTAs() {
        ui.backCtaView.setOnClickListener(
            ThrottleClick { requireActivity().onBackPressed() }
        )
        ui.setPasswordCtaTextView.setOnClickListener {
            ui.setPasswordCtaContainerView.animateClick()
            requireActivity().hideKeyboard()
            preventExitAndPasswordEditing()
            setSecurePasswordCtaClickedState()
            performBackupAndUpdatePassword()
        }
    }

    private fun preventExitAndPasswordEditing() {
        ui.backCtaView.isEnabled = false
        ui.enterPasswordEditText.isEnabled = false
        ui.confirmPasswordEditText.isEnabled = false
    }

    private fun allowExitAndPasswordEditing() {
        ui.backCtaView.isEnabled = true
        ui.enterPasswordEditText.isEnabled = true
        ui.confirmPasswordEditText.isEnabled = true
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
        val password = (ui.enterPasswordEditText.text!!.toString()).toCharArray()
        // start listening to wallet events
        subscribeToBackupState()
        lifecycleScope.launch(Dispatchers.IO) {
            // backup
            try {
                backupManager.backup(newPassword = password)
            } catch (exception: BackupStorageAuthRevokedException) {
                withContext(Dispatchers.Main) {
                    displayStorageAuthRevokedDialog()
                }
            }
        }
    }

    private fun displayStorageAuthRevokedDialog() {
        val message = string(check_backup_storage_status_auth_revoked_error_description)
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_backing_up_error_title),
            description = message,
            onClose = { requireActivity().onBackPressed() }
        ).show()
    }

    private fun subscribeToBackupState() {
        EventBus.backupState.subscribe(this) { backupState ->
            lifecycleScope.launch(Dispatchers.Main) {
                onBackupStateChanged(backupState)
            }
        }
    }

    private fun onBackupStateChanged(backupState: BackupState?) {
        if (!subscribedToBackupState) {
            // ignore first call
            Logger.d("Ignore first state: $backupState")
            subscribedToBackupState = true
            return
        }
        Logger.d("Backup state changed: $backupState")
        when (backupState) {
            is BackupUpToDate -> { // backup successful
                allowExitAndPasswordEditing()
                (requireActivity() as BackupSettingsRouter).onPasswordChanged(this)
            }
            is BackupOutOfDate -> { // backup failed
                Logger.e(
                    backupState.backupException,
                    "Error during encrypted backup: ${backupState.backupException}"
                )
                showBackupErrorDialog(deductBackupErrorMessage(backupState.backupException)) {
                    allowExitAndPasswordEditing()
                    setSecurePasswordCtaIdleState()
                }
            }
        }
    }

    private fun deductBackupErrorMessage(e: Exception?): String = when {
        e is UnknownHostException -> string(error_no_connection_title)
        e?.message != null -> string(back_up_wallet_backing_up_error_desc, e.message!!)
        else -> string(back_up_wallet_backing_up_unknown_error)
    }

    private fun showBackupErrorDialog(message: String, onClose: () -> Unit) {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_backing_up_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = onClose
        ).show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = ChangeSecurePasswordFragment()

        private const val KEYBOARD_SHOWUP_DELAY_AFTER_LOCAL_AUTH = 500L
        private const val KEYBOARD_ANIMATION_TIME = 100L
    }

}
