package com.tari.android.wallet.ui.fragment.send.requestTari

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.amountInputBinding.fragment.send.addAmount.keyboard.KeyboardController
import com.tari.android.wallet.databinding.FragmentRequestTariBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener


class RequestTariFragment : CommonFragment<FragmentRequestTariBinding, RequestTariViewModel>() {

    private val keyboardController: KeyboardController = KeyboardController()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentRequestTariBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: RequestTariViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        subscribeUI()
    }

    private fun subscribeUI() = with(viewModel) {
        observe(deeplink) { shareDeeplink(it) }
    }

    private fun setupUI() {
        keyboardController.setup(requireContext(), AmountCheckRunnable(), ui.numpad, ui.amount)
        ui.shareButton.setOnThrottledClickListener { viewModel.generateQRCodeDeeplink(keyboardController.currentAmount) }
    }

    private fun shareDeeplink(deeplink: String) {
        val subject = requireContext().getString(R.string.request_tari_share_text, keyboardController.currentAmount.tariValue.toString(), deeplink)
        ShareCompat.IntentBuilder(requireContext())
            .setText(deeplink)
            .setType("text/plain")
            .setChooserTitle(subject)
            .startChooser()
    }

    private inner class AmountCheckRunnable : Runnable {
        override fun run() {
            val amount = keyboardController.currentAmount
            val isNotEmpty = amount.tariValue.toDouble() != 0.0

            ui.shareButton.isEnabled = isNotEmpty
            ui.generateButton.isEnabled = isNotEmpty
        }
    }
}