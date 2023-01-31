package com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import com.tari.android.wallet.databinding.DialogModuleBackupOnboardingFlowItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item.*

@SuppressLint("ViewConstructor")
class BackupOnboardingFlowItemModuleView(context: Context, module: BackupOnboardingFlowItemModule) :
    CommonView<CommonViewModel, DialogModuleBackupOnboardingFlowItemBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleBackupOnboardingFlowItemBinding =
        DialogModuleBackupOnboardingFlowItemBinding.inflate(layoutInflater, parent, attachToRoot)

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