package com.tari.android.wallet.ui.fragment.settings.allSettings.button

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        ui.icon.setImageResource(R.drawable.icon_apply_setting)
        dto.iconId?.let { ui.icon.setImageResource(it) }

        val color = ContextCompat.getColor(
            context, when (dto.style) {
                ButtonStyle.Normal -> R.color.black
                ButtonStyle.Warning -> R.color.common_error
            }
        )

        val icon = when (dto.style) {
            ButtonStyle.Normal -> R.drawable.icon_apply_setting
            ButtonStyle.Warning -> R.drawable.icon_apply_setting_red
        }

        ui.title.setTextColor(color)
        ui.icon.setImageDrawable(ContextCompat.getDrawable(context, icon))
        ui.leftIcon.setColorFilter(color)

        ui.root.setOnClickListener { dto.action.invoke() }
    }
}