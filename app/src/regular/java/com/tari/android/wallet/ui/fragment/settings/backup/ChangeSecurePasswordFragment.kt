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
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentChangeSecurePasswordBinding
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine


class ChangeSecurePasswordFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var registry: SharedPrefsWrapper

    private var blockingBackPressedDispatcher = object : OnBackPressedCallback(false) {
        // No-op by design
        override fun handleOnBackPressed() = Unit
    }
    private lateinit var ui: FragmentChangeSecurePasswordBinding
    private lateinit var inputService: InputMethodManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inputService =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        backupAndRestoreComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentChangeSecurePasswordBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        ui.enterPasswordEditText.isFocusableInTouchMode = true
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressedDispatcher)
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
            UiUtil.showKeyboard(requireActivity())
            ui.enterPasswordEditText
                .postDelayed({ ui.contentScrollView.scrollToBottom() }, KEYBOARD_ANIMATION_TIME)
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
                it, !ui.confirmPasswordEditText.text.isNullOrEmpty() && !doPasswordsMatch()
            )
        }
        )
        ui.confirmPasswordEditText.addTextChangedListener(afterTextChanged = {
            updateVerifyButtonStateBasedOnEditTexts(
                it,
                !ui.enterPasswordEditText.text.isNullOrEmpty() && !doPasswordsMatch()
            )
        })
        val passwordTextFieldListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) setPlainInputState()
            else if (!hasFocus && !ui.confirmPasswordEditText.hasFocus() && areTextFieldsFilled())
                if (doPasswordsMatch()) setPlainInputState() else setPasswordMatchErrorState()
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
        val passwordsMatch = doPasswordsMatch()
        if (passwordsMatch || trigger.isNullOrEmpty()) {
            setVerifyButtonState(isEnabled = areTextFieldsFilled() && passwordsMatch)
        } else if (errorCondition) {
            setVerifyButtonState(isEnabled = false)
        }
    }

    private fun areTextFieldsFilled() = ui.enterPasswordEditText.text!!.isNotEmpty() &&
            ui.confirmPasswordEditText.text!!.isNotEmpty()

    private fun doPasswordsMatch() =
        ui.confirmPasswordEditText.text?.toString() == ui.enterPasswordEditText.text?.toString()

    private fun setPlainInputState() {
        ui.passwordsNotMatchLabelView.gone()
        ui.confirmPasswordEditText.setTextColor(color(black))
    }

    private fun setPasswordMatchErrorState() {
        ui.passwordsNotMatchLabelView.visible()
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
        setOnBackPressListener()
        ui.setPasswordCtaTextView.setOnClickListener {
            UiUtil.animateViewClick(ui.setPasswordCtaContainerView)
            UiUtil.hideKeyboard(requireActivity())
            preventExitAndPasswordEditing()
            setSecurePasswordCtaClickedState()
            performBackupAndUpdatePassword()
        }
    }

    private fun setOnBackPressListener() {
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
    }

    private fun preventExitAndPasswordEditing() {
        blockingBackPressedDispatcher.isEnabled = true
        ui.backCtaView.setOnClickListener(null)
        ui.enterPasswordEditText.isEnabled = false
        ui.confirmPasswordEditText.isEnabled = false
    }

    private fun allowExitAndPasswordEditing() {
        blockingBackPressedDispatcher.isEnabled = false
        setOnBackPressListener()
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
        lifecycleScope.launch {
            try {
                performBackup()
                registry.lastSuccessfulBackupDateTime = DateTime.now()
                registry.backupPassword = ui.enterPasswordEditText.text!!
                    .run { CharArray(length).also { getChars(0, length, it, 0) } }
                allowExitAndPasswordEditing()
                (requireActivity() as SettingsRouter).onPasswordChanged()
            } catch (e: Exception) {
                Logger.e(e, "Error occurred during backup from change secure password page: $e")
                showBackupErrorDialog(deductBackupErrorMessage(e)) {
                    allowExitAndPasswordEditing()
                    setSecurePasswordCtaIdleState()
                }
            }
        }
    }

    private fun deductBackupErrorMessage(e: Exception): String = when {
        e is UnknownHostException -> string(error_no_connection_title)
        e.message != null -> string(back_up_wallet_backing_up_error_desc, e.message!!)
        else -> string(back_up_wallet_backing_up_unknown_error)
    }

    private suspend fun performBackup() {
        val viewModel =
            ViewModelProvider(requireActivity()).get(StorageBackupViewModel::class.java)
        suspendCoroutine<Unit> { c ->
            viewModel.state.observe(viewLifecycleOwner, Observer {
                when (it.processStatus) {
                    BackupProcessStatus.SUCCESS, BackupProcessStatus.FAILURE -> {
                        viewModel.state.removeObservers(viewLifecycleOwner)
                        viewModel.resetProcessStatus()
                        val isSuccess = it.processStatus == BackupProcessStatus.SUCCESS
                        c.resumeWith(
                            if (isSuccess) Result.success(Unit)
                            else Result.failure(it.processException!!)
                        )
                    }
                    else -> {
                    }
                }
            })
            viewModel.backup(ui.enterPasswordEditText.text.toString().toCharArray())
        }

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
