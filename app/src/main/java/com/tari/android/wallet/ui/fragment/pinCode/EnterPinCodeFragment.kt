package com.tari.android.wallet.ui.fragment.pinCode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentEnterPincodeBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.auth.AuthActivity
import com.tari.android.wallet.ui.fragment.auth.FeatureAuthFragment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EnterPinCodeFragment : CommonFragment<FragmentEnterPincodeBinding, EnterPinCodeViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentEnterPincodeBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: EnterPinCodeViewModel by viewModels()
        bindViewModel(viewModel)

        val behavior = arguments?.getInt(PIN_CODE_EXTRA_KEY)?.let { PinCodeScreenBehavior.values()[it] } ?: PinCodeScreenBehavior.CreateConfirm
        val stashedPinCode = arguments?.getString(PIN_CODE_STASHED_KEY)
        viewModel.init(behavior, stashedPinCode)

        initUI()
        ui.continueBtn.setOnThrottledClickListener { viewModel.doMainAction() }
        observeUI()
    }

    override fun onStart() {
        super.onStart()
        viewModel.currentNums.value = ""
    }

    private fun initUI() = with(ui.numpad) {
        pad0Button.setOnClickListener { viewModel.addNum("0") }
        pad1Button.setOnClickListener { viewModel.addNum("1") }
        pad2Button.setOnClickListener { viewModel.addNum("2") }
        pad3Button.setOnClickListener { viewModel.addNum("3") }
        pad4Button.setOnClickListener { viewModel.addNum("4") }
        pad5Button.setOnClickListener { viewModel.addNum("5") }
        pad6Button.setOnClickListener { viewModel.addNum("6") }
        pad7Button.setOnClickListener { viewModel.addNum("7") }
        pad8Button.setOnClickListener { viewModel.addNum("8") }
        pad9Button.setOnClickListener { viewModel.addNum("9") }
        deleteButton.setOnClickListener { viewModel.removeLast() }
        biometricAuth.setOnClickListener {
            (requireActivity() as? AuthActivity)?.showBiometricAuth()
            requireActivity().supportFragmentManager.fragments.firstOrNull { it is FeatureAuthFragment }?.let {
                (it as? FeatureAuthFragment)?.showBiometricAuth()
            }
        }
        decimalPointButton.gone()
    }

    private fun observeUI() = with(viewModel) {
        observe(behavior) {
            val isVisible =
                (it == PinCodeScreenBehavior.Auth || it == PinCodeScreenBehavior.FeatureAuth) && viewModel.securityPrefRepository.biometricsAuth == true
            ui.numpad.biometricAuth.setVisible(isVisible)
            if (isVisible) {
                ui.numpad.decimalPointButton.gone()
            } else {
                ui.numpad.decimalPointButton.invisible()
            }
        }

        observe(subtitleText) {
            ui.subtitle.text = it
            ui.toolbar.ui.toolbarTitle.text = it
        }

        observe(toolbarIsVisible) { ui.toolbar.setVisible(it) }

        observe(buttonIsVisible) { ui.continueBtn.setVisible(it) }

        observe(buttonTitle) { ui.continueBtn.text = it }

        observe(currentNums) {
            val length = it.length
            ui.pinCode1.text = if (length > 0) "●" else ""
            ui.pinCode2.text = if (length > 1) "●" else ""
            ui.pinCode3.text = if (length > 2) "●" else ""
            ui.pinCode4.text = if (length > 3) "●" else ""
            ui.pinCode5.text = if (length > 4) "●" else ""
            ui.pinCode6.text = if (length > 5) "●" else ""
            ui.continueBtn.isEnabled = length >= 6
            if (length < 6) {
                viewModel.errorMessage.postValue("")
            }
        }

        observe(errorMessage) {
            setState(it.isNotEmpty())
            ui.errorMessage.text = it
            ui.errorMessage.setVisible(it.isNotEmpty())
        }

        observe(successfullyAuth) {
            (requireActivity() as? AuthActivity)?.continueToHomeActivity()
            requireActivity().supportFragmentManager.fragments.firstOrNull { it is FeatureAuthFragment }?.let {
                (it as? FeatureAuthFragment)?.authSuccessfully()
            }
        }

        observe(nextEnterTime) {
            val nextEnterTime = viewModel.nextEnterTime.value
            val now = LocalDateTime.now()
            setSecurityState(now.isBefore(nextEnterTime))
            val formatter = DateTimeFormatter.ofPattern("hh:mm", Locale.ENGLISH)
            val errorText = if (now.isBefore(nextEnterTime)) "Too many attempts. You should wait ${formatter.format(nextEnterTime)}" else ""
            ui.errorMessage.text = errorText
        }
    }

    private fun setSecurityState(isDisabled: Boolean) = with(ui) {
        pinCodeContainer.setVisible(!isDisabled)
        numpad.root.alpha = if (isDisabled) 0.5f else 1f
        for (index in 0 until numpad.root.childCount) {
            numpad.root.getChildAt(index).isEnabled = !isDisabled
        }
    }

    private fun setState(isError: Boolean) {
        val textColor = if (isError) viewModel.paletteManager.getRed(requireContext()) else viewModel.paletteManager.getTextHeading(requireContext())
        val allPinCodeViews = listOf(ui.pinCode1, ui.pinCode2, ui.pinCode3, ui.pinCode4, ui.pinCode5, ui.pinCode6)
        allPinCodeViews.forEach { it.setTextColor(textColor) }
    }

    companion object {
        const val PIN_CODE_EXTRA_KEY = "pin_code_extra_key"
        const val PIN_CODE_STASHED_KEY = "pin_code_stashed_key"

        fun newInstance(behavior: PinCodeScreenBehavior, pinCodeStashed: String? = null) = EnterPinCodeFragment().apply {
            arguments = Bundle().apply {
                putInt(PIN_CODE_EXTRA_KEY, behavior.ordinal)
                putString(PIN_CODE_STASHED_KEY, pinCodeStashed)
            }
        }
    }
}