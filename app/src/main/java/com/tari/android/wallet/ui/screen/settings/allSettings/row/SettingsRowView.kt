package com.tari.android.wallet.ui.screen.settings.allSettings.row

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewSettingsRowBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible

class SettingsRowView : CommonView<CommonViewModel, ViewSettingsRowBinding> {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewSettingsRowBinding =
        ViewSettingsRowBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    fun initDto(dto: SettingsRowViewHolderItem) {
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
            SettingsRowStyle.Normal -> PaletteManager.getTextHeading(context)
            SettingsRowStyle.Warning -> PaletteManager.getRed(context)
        }

        ui.title.setTextColor(color)
        ui.icon.setColorFilter(color)
        ui.leftIcon.setColorFilter(color)

        ui.warning.setVisible(dto.warning)

        ui.root.setOnClickListener { dto.action.invoke() }
    }
}