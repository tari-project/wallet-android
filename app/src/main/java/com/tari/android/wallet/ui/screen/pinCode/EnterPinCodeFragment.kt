package com.tari.android.wallet.ui.screen.pinCode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentEnterPincodeBinding
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.auth.AuthActivity
import com.tari.android.wallet.ui.screen.auth.FeatureAuthFragment
import com.tari.android.wallet.util.extension.addTo
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.setOnThrottledClickListener
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.visible
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class EnterPinCodeFragment : CommonXmlFragment<FragmentEnterPincodeBinding, EnterPinCodeViewModel>() {

    override fun screenRecordingAlwaysDisable() = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentEnterPincodeBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: EnterPinCodeViewModel by viewModels()
        bindViewModel(viewModel)

        val behavior = arguments?.getInt(PIN_CODE_EXTRA_KEY)?.let { PinCodeScreenBehavior.entries[it] } ?: PinCodeScreenBehavior.CreateConfirm
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
        initNumpad()
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
            ui.pinCode1Dot.setVisible(length > 0)
            ui.pinCode2Dot.setVisible(length > 1)
            ui.pinCode3Dot.setVisible(length > 2)
            ui.pinCode4Dot.setVisible(length > 3)
            ui.pinCode5Dot.setVisible(length > 4)
            ui.pinCode6Dot.setVisible(length > 5)
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
            val isLater = now.isBefore(nextEnterTime)
            setSecurityState(isLater)
            ui.errorMessageCountdown.setVisible(isLater)
            ui.errorMessage.setVisible(!isLater)
            startCountdown(nextEnterTime)
        }
    }

    private fun startCountdown(dateTime: LocalDateTime?) {
        if (dateTime != null && dateTime.isAfter(LocalDateTime.now())) {
            updateText(Duration.between(LocalDateTime.now(), dateTime))
            // TODO use coroutines!!
            Observable.timer(1, TimeUnit.SECONDS)
                .repeat()
                .map { Duration.between(LocalDateTime.now(), dateTime) }
                .takeWhile {
                    val shouldTake = LocalDateTime.now().isBefore(dateTime)
                    if (!shouldTake) {
                        viewModel.nextEnterTime.postValue(LocalDateTime.now())
                    }
                    shouldTake
                }
                .subscribe { lifecycleScope.launch(Dispatchers.Main) { updateText(it) } }
                .addTo(viewModel.compositeDisposable)
        }
    }

    private fun updateText(duration: Duration) {
        val seconds = duration.seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        var timeText = ""
        if (hours > 0) {
            timeText += "${hours}h "
        }
        if (minutes > 0) {
            timeText += "${minutes % 60}m "
        }
        timeText += "${seconds % 60}s"
        val errorText = if (!duration.isNegative) "Too many attempts. You should wait $timeText" else ""
        ui.errorMessageCountdown.text = errorText
        ui.errorMessageCountdown.visible()
        ui.errorMessage.invisible()
    }

    private fun setSecurityState(isDisabled: Boolean) = with(ui) {
        pinCodeContainer.setVisible(!isDisabled)
        subtitle.setVisible(!isDisabled)
        for (index in 0 until numpad.root.childCount) {
            numpad.root.getChildAt(index).alpha = if (isDisabled) 0.5f else 1f
        }
        if (isDisabled) clearNumpad() else initNumpad()
        numpad.biometricAuth.alpha = 1f
    }

    private fun setState(isError: Boolean) {
        val backResource = if (isError) R.drawable.vector_pin_input_dot_error else R.drawable.vector_pin_input_dot_normal
        val allPinCodeViews = listOf(ui.pinCode1Dot, ui.pinCode2Dot, ui.pinCode3Dot, ui.pinCode4Dot, ui.pinCode5Dot, ui.pinCode6Dot)
        allPinCodeViews.forEach { it.setBackgroundResource(backResource) }
    }

    private fun initNumpad() = with(ui.numpad) {
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
    }

    private fun clearNumpad() = with(ui.numpad) {
        pad0Button.setOnClickListener { }
        pad1Button.setOnClickListener { }
        pad2Button.setOnClickListener { }
        pad3Button.setOnClickListener { }
        pad4Button.setOnClickListener { }
        pad5Button.setOnClickListener { }
        pad6Button.setOnClickListener { }
        pad7Button.setOnClickListener { }
        pad8Button.setOnClickListener { }
        pad9Button.setOnClickListener { }
        deleteButton.setOnClickListener { }
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