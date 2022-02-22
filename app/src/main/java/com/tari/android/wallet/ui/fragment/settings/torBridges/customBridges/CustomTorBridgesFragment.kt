package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentCustomTorBridgesBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsRouter

class CustomTorBridgesFragment : CommonFragment<FragmentCustomTorBridgesBinding, CustomTorBridgesViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentCustomTorBridgesBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: CustomTorBridgesViewModel by viewModels()
        bindViewModel(viewModel)

        setupViews()

        observeUI()
    }

    private fun setupViews() = with(ui) {
        backCtaView.setOnThrottledClickListener { requireActivity().onBackPressed() }
        requestBridgesCta.setOnThrottledClickListener { viewModel.openRequestPage() }
        scanQrCta.setOnThrottledClickListener { viewModel.navigateToScanQr() }
        uploadQrCta.setOnThrottledClickListener { viewModel.navigateToUploadQr() }
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }
    }

    private fun processNavigation(navigation: CustomBridgeNavigation) {
        val router = requireActivity() as AllSettingsRouter
        when(navigation) {
            CustomBridgeNavigation.ScanQrCode -> router.toScanQrCodeBridge()
            CustomBridgeNavigation.UploadQrCode -> router.toUploadQrCodeBridge()
        }
    }
}

