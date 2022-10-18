package com.tari.android.wallet.ui.dialog.modular.modules.checked

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleCheckedBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class CheckedModuleView(context: Context, checkedModule: CheckedModule) : CommonView<CommonViewModel, DialogModuleCheckedBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleCheckedBinding =
        DialogModuleCheckedBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.title.text = checkedModule.text
        ui.switcher.isChecked = checkedModule.isChecked
        ui.switcher.setOnCheckedChangeListener { _, isChecked -> checkedModule.isChecked = isChecked }
    }
}