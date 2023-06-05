package com.tari.android.wallet.ui.dialog.modular.modules.icon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleIconBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

class IconModuleView : CommonView<CommonViewModel, DialogModuleIconBinding> {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, buttonModule: IconModule) : this(context) {
        initItem(buttonModule)
    }

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleIconBinding =
        DialogModuleIconBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    fun initItem(iconModule: IconModule) {
        ui.moduleIcon.setImageResource(iconModule.icon)
    }
}