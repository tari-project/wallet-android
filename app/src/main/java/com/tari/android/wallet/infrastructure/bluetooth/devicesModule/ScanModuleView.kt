package com.tari.android.wallet.infrastructure.bluetooth.devicesModule

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleBleDevicesBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class ScanModuleView(context: Context, val scanModule: ScanModule) : CommonView<CommonViewModel, DialogModuleBleDevicesBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleBleDevicesBinding =
        DialogModuleBleDevicesBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit
}