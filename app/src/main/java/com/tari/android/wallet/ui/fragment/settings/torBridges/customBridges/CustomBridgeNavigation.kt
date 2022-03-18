package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

sealed class CustomBridgeNavigation {
    object ScanQrCode: CustomBridgeNavigation()

    object UploadQrCode: CustomBridgeNavigation()
}