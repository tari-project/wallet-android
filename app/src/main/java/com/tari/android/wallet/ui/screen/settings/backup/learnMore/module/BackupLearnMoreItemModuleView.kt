package com.tari.android.wallet.ui.screen.settings.backup.learnMore.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import com.tari.android.wallet.databinding.DialogModuleBackupLearnMoreItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible

@SuppressLint("ViewConstructor")
class BackupLearnMoreItemModuleView(context: Context, module: BackupLearnMoreItemModule) :
    CommonView<CommonViewModel, DialogModuleBackupLearnMoreItemBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleBackupLearnMoreItemBinding =
        DialogModuleBackupLearnMoreItemBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        with(ui.item) {
            val args = module.stageArgs
            imageView.setImageResource(args.image)
            title.text = args.title
            description.text = args.description
            moduleButton.initItem(args.button) { }
            textBelow.text = args.bottomText

            divider.setVisible(args.bottomText.isNotEmpty())
            textBelow.setVisible(args.bottomText.isNotEmpty())
        }
        ui.closeButton.setOnClickListener { module.dismissAction() }
    }
}