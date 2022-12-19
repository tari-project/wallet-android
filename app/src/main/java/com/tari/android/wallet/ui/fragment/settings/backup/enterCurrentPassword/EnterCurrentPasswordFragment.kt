package com.tari.android.wallet.ui.fragment.settings.backup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R.color.change_password_cta_disabled
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.databinding.FragmentEnterBackupPasswordBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import javax.inject.Inject

class EnterCurrentPasswordFragment : Fragment() {

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    private lateinit var ui: FragmentEnterBackupPasswordBinding

    private var canEnableChangePasswordCTA = true

    private val passwordInput
        get() = ui.passwordEditText.ui.editText

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentEnterBackupPasswordBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setChangePasswordCTAState(isEnabled = false)
        ui.passwordEditText.requestFocus()
        ui.root.postDelayed(LOCAL_AUTH_DELAY_TIME) { requireActivity().showKeyboard() }
        ui.changePasswordCtaTextView.setOnClickListener {
            val input = (ui.passwordEditText.text?.toString() ?: "").toCharArray()
            val backupPassword = backupSettingsRepository.backupPassword?.toCharArray() ?: charArrayOf()
            if (input.contentEquals(backupPassword)) {
                (requireActivity() as BackupSettingsRouter).toChangePassword()
            } else {
                ui.changePasswordCtaTextView.isEnabled = false
                ui.changePasswordCtaTextView.setTextColor(color(change_password_cta_disabled))
                canEnableChangePasswordCTA = false
                ui.root.postDelayed(DISABLE_BUTTON_TIME) {
                    canEnableChangePasswordCTA = true
                    if (!ui.passwordEditText.text.isNullOrBlank()) {
                        ui.changePasswordCtaTextView.isEnabled = true
                        ui.changePasswordCtaTextView.setTextColor(color(white))
                    }
                }
                ui.passwordsNotMatchLabelView.visible()
            }
        }
        ui.passwordEditText.addTextChangedListener(
            afterTextChanged = {
                setChangePasswordCTAState(canEnableChangePasswordCTA && (it?.length ?: 0) != 0)
                ui.passwordsNotMatchLabelView.gone()
            }
        )
        ui.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
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
