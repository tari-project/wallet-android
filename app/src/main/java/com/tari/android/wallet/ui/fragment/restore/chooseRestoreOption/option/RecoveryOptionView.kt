package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewRestoreOptionBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setColor
import com.tari.android.wallet.ui.extension.setVisible

class RecoveryOptionView : CommonView<CommonViewModel, ViewRestoreOptionBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewRestoreOptionBinding = ViewRestoreOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun init(title: String) {
        ui.title.text = title
    }

    fun updateLoading(isLoading: Boolean) {
        ui.restoreWalletMenuItemProgressView.setVisible(isLoading)
        ui.restoreWalletMenuItemArrowImageView.setVisible(!isLoading)
        ui.restoreWalletCtaView.isEnabled = false
    }

    override fun setup() {
        ui.restoreWalletMenuItemProgressView.setColor(color(R.color.all_settings_back_up_status_processing))
        ui.restoreWalletMenuItemProgressView.gone()
    }
}