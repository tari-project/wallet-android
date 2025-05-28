package com.tari.android.wallet.ui.dialog.modular.modules.addressDetails

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.databinding.DialogModuleAddressDetailsBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TariWalletAddress.Feature
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.string

@SuppressLint("ViewConstructor")
class AddressDetailsModuleView(context: Context, addressDetailsModule: AddressDetailsModule) :
    CommonView<CommonViewModel, DialogModuleAddressDetailsBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleAddressDetailsBinding =
        DialogModuleAddressDetailsBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        addressDetailsModule.tariWalletAddress.let { address ->
            ui.textNetwork.text = address.networkName()
            ui.textFeatures.text = address.featuresNames()
            ui.containerViewKey.setVisible(address.viewKeyEmojis != null)
            ui.textViewKeyHeading.setVisible(address.viewKeyEmojis != null)
            ui.textViewKey.text = address.viewKeyEmojis
            ui.textSpendKey.text = address.spendKeyEmojis
            ui.textViewChecksum.text = address.checksumEmoji
            ui.buttonCopyBase58.setOnClickListener { addressDetailsModule.copyBase58() }
            ui.buttonCopyEmojis.setOnClickListener { addressDetailsModule.copyEmojis() }
        }
    }

    private fun TariWalletAddress.networkName(): String = "${this.networkEmoji} " + when (this.network) {
        TariWalletAddress.Network.MAINNET -> Network.MAINNET.displayName
        TariWalletAddress.Network.STAGENET -> Network.STAGENET.displayName
        TariWalletAddress.Network.NEXTNET -> Network.NEXTNET.displayName
        TariWalletAddress.Network.TESTNET -> "TestNet"
    }

    private fun TariWalletAddress.featuresNames(): String = "${this.featuresEmoji} " + this.features.joinToString(", ") {
        when (it) {
            Feature.ONE_SIDED -> string(R.string.wallet_info_address_one_side_payment)
            Feature.INTERACTIVE -> string(R.string.wallet_info_address_interactive_payment)
            Feature.PAYMENT_ID -> string(R.string.wallet_info_address_safetrade)
        }
    }
}

