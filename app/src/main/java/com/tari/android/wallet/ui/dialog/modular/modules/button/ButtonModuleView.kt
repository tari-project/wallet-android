package com.tari.android.wallet.ui.dialog.modular.modules.button

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleButtonBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener

@SuppressLint("ViewConstructor")
class ButtonModuleView(context: Context, buttonModule: ButtonModule, dismissAction: () -> Unit) :
    CommonView<CommonViewModel, DialogModuleButtonBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleButtonBinding =
        DialogModuleButtonBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.button.text = buttonModule.text
        when (buttonModule.style) {
            ButtonStyle.Normal -> {
                ui.button.setTextColor(ContextCompat.getColor(context, R.color.white))
                ui.button.background = ContextCompat.getDrawable(context, R.drawable.disableable_gradient_button_bg)
            }
            ButtonStyle.Warning -> {
                ui.button.setTextColor(ContextCompat.getColor(context, R.color.white))
                ui.button.background = ContextCompat.getDrawable(context, R.drawable.destructive_action_button_bg)
            }
            ButtonStyle.Close -> {
                ui.button.setTextColor(ContextCompat.getColor(context, R.color.purple))
                ui.button.background = null
            }
        }
        val dimen = if (buttonModule.style == ButtonStyle.Close) R.dimen.common_action_button_close_height else R.dimen.common_action_button_height
        ui.button.layoutParams.height = context.dimenPx(dimen)
        ui.button.requestLayout()
        ui.button.setOnThrottledClickListener {
            buttonModule.action()
            if (buttonModule.style == ButtonStyle.Close) {
                dismissAction()
            }
        }
    }
}