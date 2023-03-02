package com.tari.android.wallet.ui.dialog.modular

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.securityStages.modules.SecurityStageHeadModule
import com.tari.android.wallet.data.sharedPrefs.securityStages.modules.SecurityStageHeadModuleView
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModule
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModuleView
import com.tari.android.wallet.ui.component.tari.TariPrimaryBackground
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModule
import com.tari.android.wallet.ui.dialog.modular.modules.checked.CheckedModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody.CustomBaseNodeBodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody.CustomBaseNodeBodyModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadBoldSpannableModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadBoldSpannableModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadSpannableModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadSpannableModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModule
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModuleView
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.FeeModule
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.FeeModuleView
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQRCodeModuleView
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModule
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModuleView
import com.tari.android.wallet.ui.fragment.settings.logs.logs.module.LogLevelCheckedModule
import com.tari.android.wallet.ui.fragment.settings.logs.logs.module.LogSourceCheckedModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.DetailItemModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.DetailItemModuleView
import com.tari.android.wallet.ui.fragment.utxos.list.module.ListItemModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.ListItemModuleView
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoAmountModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoAmountModuleView
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoSplitModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoSplitModuleView


class InputModularDialog(context: Context) : ModularDialog(context) {

    constructor(context: Context, args: ModularDialogArgs) : this(context) {
        applyArgs(args)
        modifyDialog()
    }

    private fun modifyDialog() {
        dialog.findViewById<TariPrimaryBackground>(R.id.root)?.apply {
            elevation = 0F
            updateBack(0F, 0F)
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(0, 0, 0, 0)
            }
        }
    }
}