package com.tari.android.wallet.ui.fragment.settings.allSettings.button

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewButtonBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setVisible

class ButtonView : CommonView<CommonViewModel, ViewButtonBinding> {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewButtonBinding =
        ViewButtonBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    fun initDto(dto: ButtonViewDto) {
        ui.leftIcon.setVisible(dto.leftIconId != null)
        dto.leftIconId?.let { ui.leftIcon.setImageResource(it) }
        ui.title.text = dto.title
        ui.icon.setImageResource(R.drawable.vector_back_button)
        ui.icon.rotation = 180.0F
        dto.iconId?.let {
            ui.icon.setImageResource(it)
            ui.icon.rotation = 0.0F
        }

        val color = when (dto.style) {
            ButtonStyle.Normal -> paletteManager.getTextHeading(context)
            ButtonStyle.Warning -> paletteManager.getRed(context)
        }

        ui.title.setTextColor(color)
        ui.icon.setColorFilter(color)
        ui.leftIcon.setColorFilter(color)

        ui.root.setOnClickListener { dto.action.invoke() }
    }
}