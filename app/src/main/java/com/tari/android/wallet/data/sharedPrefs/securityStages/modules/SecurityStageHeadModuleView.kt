package com.tari.android.wallet.data.sharedPrefs.securityStages.modules

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleSecurityStageTitleBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class SecurityStageHeadModuleView(context: Context, buttonModule: SecurityStageHeadModule) :
    CommonView<CommonViewModel, DialogModuleSecurityStageTitleBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleSecurityStageTitleBinding =
        DialogModuleSecurityStageTitleBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.emojiTitle.text = buttonModule.emojiTitle
        ui.title.text = buttonModule.title
    }
}