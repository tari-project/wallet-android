package com.tari.android.wallet.ui.fragment.settings.backup.enterCurrentPassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentEnterCurrentPasswordBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.showKeyboard
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRouter

class EnterCurrentPasswordFragment : CommonFragment<FragmentEnterCurrentPasswordBinding, EnterCurrentPasswordViewModel>() {

    private var canEnableChangePasswordCTA = true

    private val passwordInput
        get() = ui.passwordEditText.ui.editText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentEnterCurrentPasswordBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: EnterCurrentPasswordViewModel by viewModels()
        bindViewModel(viewModel)

        setChangePasswordCTAState(isEnabled = false)
        passwordInput.requestFocus()
        ui.root.postDelayed(LOCAL_AUTH_DELAY_TIME) { requireActivity().showKeyboard() }
        ui.changePasswordCtaTextView.setOnClickListener {
            val input = (passwordInput.text?.toString() ?: "").toCharArray()
            val backupPassword = viewModel.backupSettingsRepository.backupPassword?.toCharArray() ?: charArrayOf()
            if (input.contentEquals(backupPassword)) {
                (requireActivity() as BackupSettingsRouter).toChangePassword()
            } else {
                ui.changePasswordCtaTextView.isEnabled = false
                canEnableChangePasswordCTA = false
                ui.root.postDelayed(DISABLE_BUTTON_TIME) {
                    canEnableChangePasswordCTA = true
                    if (!passwordInput.text.isNullOrBlank()) {
                        ui.changePasswordCtaTextView.isEnabled = true
                    }
                }
                ui.passwordsNotMatchLabelView.visible()
            }
        }
        passwordInput.addTextChangedListener(
            afterTextChanged = {
                setChangePasswordCTAState(canEnableChangePasswordCTA && (it?.length ?: 0) != 0)
                ui.passwordsNotMatchLabelView.gone()
            }
        )
        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) ui.passwordsNotMatchLabelView.gone()
        }
    }

    private fun setChangePasswordCTAState(isEnabled: Boolean) {
        ui.changePasswordCtaTextView.isEnabled = isEnabled
    }

    companion object {
        private const val DISABLE_BUTTON_TIME = 1000L
        private const val LOCAL_AUTH_DELAY_TIME = 500L
    }
}

