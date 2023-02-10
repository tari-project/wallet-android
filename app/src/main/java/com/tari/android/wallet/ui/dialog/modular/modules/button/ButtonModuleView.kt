package com.tari.android.wallet.ui.dialog.modular.modules.button

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleButtonBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener

class ButtonModuleView : CommonView<CommonViewModel, DialogModuleButtonBinding> {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, buttonModule: ButtonModule, dismissAction: () -> Unit) : this(context) {
        initItem(buttonModule, dismissAction)
    }

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleButtonBinding =
        DialogModuleButtonBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    fun initItem(buttonModule: ButtonModule, dismissAction: () -> Unit) {
        ui.button.text = buttonModule.text
        when (buttonModule.style) {
            ButtonStyle.Normal -> {
                ui.button.setTextColor(paletteManager.getButtonPrimaryText(context))
                ui.button.background = ContextCompat.getDrawable(context, R.drawable.vector_disable_able_gradient_button_bg)
            }

            ButtonStyle.Warning -> {
                ui.button.setTextColor(paletteManager.getButtonPrimaryText(context))
                ui.button.background = ContextCompat.getDrawable(context, R.drawable.vector_destructive_action_button_bg)
            }

            ButtonStyle.Close -> {
                ui.button.setTextColor(paletteManager.getOverlayText(context))
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