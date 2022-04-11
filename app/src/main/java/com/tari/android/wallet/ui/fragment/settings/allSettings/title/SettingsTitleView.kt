package com.tari.android.wallet.ui.fragment.settings.allSettings.title

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.ViewSettingsTitleBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

class SettingsTitleView : CommonView<CommonViewModel, ViewSettingsTitleBinding> {

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewSettingsTitleBinding =
        ViewSettingsTitleBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    fun initDto(dto: SettingsTitleDto) {
        ui.title.text = dto.title
    }
}