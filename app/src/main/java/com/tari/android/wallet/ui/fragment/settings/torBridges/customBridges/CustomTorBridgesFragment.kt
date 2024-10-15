package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.databinding.FragmentCustomTorBridgesBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource

class CustomTorBridgesFragment : CommonFragment<FragmentCustomTorBridgesBinding, CustomTorBridgesViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentCustomTorBridgesBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: CustomTorBridgesViewModel by viewModels()
        bindViewModel(viewModel)

        setupViews()
    }

    private fun setupViews() = with(ui) {
        requestBridgesCta.setOnThrottledClickListener { viewModel.openRequestPage() }
        scanQrCta.setOnThrottledClickListener {
            QrScannerActivity.startScanner(this@CustomTorBridgesFragment, QrScannerSource.TorBridges)
        }
        uploadQrCta.setOnThrottledClickListener { viewModel.navigateToUploadQr() }
        val actionArgs = TariToolbarActionArg(title = requireContext().getString(R.string.tor_bridges_connect)) {
            viewModel.connect(torBridgeConfiguration.ui.editText.text.toString())
        }
        toolbar.setRightArgs(actionArgs)

        observe(viewModel.text) { torBridgeConfiguration.ui.editText.setText(it) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleQrCode(qrDeepLink)
        }
    }
}