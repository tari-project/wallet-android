package com.tari.android.wallet.ui.dialog.modular

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModule
import com.tari.android.wallet.ui.component.networkStateIndicator.module.ConnectionStatusesModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModule
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.AddressPoisoningModule
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.AddressPoisoningModuleView
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
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModule
import com.tari.android.wallet.ui.dialog.modular.modules.option.OptionModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.securityStages.SecurityStageHeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.securityStages.SecurityStageHeadModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.shareOptions.ShareOptionsModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareOptions.ShareOptionsModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiModuleView
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModuleView
import com.tari.android.wallet.util.extension.isStillAlive
import com.tari.android.wallet.ui.screen.send.addAmount.feeModule.FeeModule
import com.tari.android.wallet.ui.screen.send.addAmount.feeModule.FeeModuleView
import com.tari.android.wallet.ui.screen.send.shareQr.ShareQRCodeModuleView
import com.tari.android.wallet.ui.screen.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModule
import com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModuleView
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogLevelCheckedModule
import com.tari.android.wallet.ui.screen.settings.logs.logs.module.LogSourceCheckedModule
import com.tari.android.wallet.ui.screen.utxos.list.module.DetailItemModule
import com.tari.android.wallet.ui.screen.utxos.list.module.DetailItemModuleView
import com.tari.android.wallet.ui.screen.utxos.list.module.ListItemModule
import com.tari.android.wallet.ui.screen.utxos.list.module.ListItemModuleView
import com.tari.android.wallet.ui.screen.utxos.list.module.UtxoAmountModule
import com.tari.android.wallet.ui.screen.utxos.list.module.UtxoAmountModuleView
import com.tari.android.wallet.ui.screen.utxos.list.module.UtxoSplitModule
import com.tari.android.wallet.ui.screen.utxos.list.module.UtxoSplitModuleView
import java.lang.ref.WeakReference

open class ModularDialog(context: Activity) {
    private val weakContext = WeakReference(context)

    lateinit var args: ModularDialogArgs

    // TODO we never clear listeners nor stop animations. May cause memory leaks.
    private val onDismissListeners = mutableListOf<() -> Unit>()

    constructor(context: Activity, args: ModularDialogArgs) : this(context) {
        applyArgs(args)
    }

    val dialog: Dialog = Dialog(context, R.style.BottomSlideDialog).apply {
        setContentView(R.layout.dialog_base)
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            it.setGravity(Gravity.BOTTOM)
        }
    }

    fun show() {
        withContext {
            dialog.show()
            showAnimation(true)
        }
    }

    /**
     * forceDismiss - dismiss dialog without animation
     */
    fun dismiss(forceDismiss: Boolean = false) {
        if (forceDismiss) {
            dialog.dismiss()
        } else {
            withContext {
                showAnimation(false) {
                    runCatching {
                        dialog.dismiss()
                    }
                }
            }
        }
    }

    fun addDismissListener(onDismiss: () -> Unit) {
        onDismissListeners.add(onDismiss)
    }

    fun applyArgs(args: ModularDialogArgs) {
        withContext { context ->
            this.args = args
            with(dialog) {
                setCancelable(args.dialogArgs.cancelable)
                setCanceledOnTouchOutside(args.dialogArgs.canceledOnTouchOutside)
                setOnDismissListener {
                    onDismissListeners.forEach { runCatching { it() } }
                    args.dialogArgs.onDismiss()
                }
                if (args.dialogArgs.canceledOnTouchOutside) {
                    findViewById<View>(R.id.back).setOnClickListener {
                        this@ModularDialog.dismiss()
                    }
                }
            }
            updateModules(context, args.modules)
        }
    }

    private fun updateModules(context: Activity, modules: List<IDialogModule>) {
        val root = dialog.findViewById<LinearLayoutCompat>(R.id.dialog_root_view)
        root.removeAllViews()
        for (module in modules) {
            module.dismissAction = dialog::dismiss
            val view = when (module) {
                is SpaceModule -> SpaceModuleView(context, module)
                is HeadModule -> HeadModuleView(context, module)
                is HeadSpannableModule -> HeadSpannableModuleView(context, module)
                is HeadBoldSpannableModule -> HeadBoldSpannableModuleView(context, module)
                is ImageModule -> ImageModuleView(context, module)
                is BodyModule -> BodyModuleView(context, module)
                is OptionModule -> OptionModuleView(context, module)
                is ButtonModule -> ButtonModuleView(context, module) { dialog.dismiss() }
                is CheckedModule -> CheckedModuleView(context, module)
                is LogSourceCheckedModule -> CheckedModuleView(context, module.checkedModule)
                is LogLevelCheckedModule -> CheckedModuleView(context, module.checkedModule)
                is CustomBaseNodeBodyModule -> CustomBaseNodeBodyModuleView(context, module)
                is ShareQrCodeModule -> ShareQRCodeModuleView(context, module)
                is FeeModule -> FeeModuleView(context, module)
                is ListItemModule -> ListItemModuleView(context, module)
                is DetailItemModule -> DetailItemModuleView(context, module)
                is UtxoAmountModule -> UtxoAmountModuleView(context, module)
                is UtxoSplitModule -> UtxoSplitModuleView(context, module)
                is ConnectionStatusesModule -> ConnectionStatusesModuleView(context, module)
                is SecurityStageHeadModule -> SecurityStageHeadModuleView(context, module)
                is BackupOnboardingFlowItemModule -> BackupOnboardingFlowItemModuleView(context, module)
                is InputModule -> InputModuleView(context, module)
                is ShortEmojiIdModule -> ShortEmojiModuleView(context, module)
                is IconModule -> IconModuleView(context, module)
                is AddressPoisoningModule -> AddressPoisoningModuleView(context, module)
                is AddressDetailsModule -> AddressDetailsModuleView(context, module)
                is ShareOptionsModule -> ShareOptionsModuleView(context, module)
                else -> View(context)
            }
            root.addView(view)
        }
    }

    private fun showAnimation(forward: Boolean, endAction: () -> Unit = {}) {
        val back = dialog.findViewById<View>(R.id.back)
        val content = dialog.findViewById<View>(R.id.root)

        val backAlpha = 0.13F
        val start = if (forward) 0.0F else 1.0F
        val end = if (forward) 1.0F else 0.0F
        ValueAnimator.ofFloat(start, end).apply {
            duration = 400
            addUpdateListener {
                val value = it.animatedValue as Float
                back.alpha = value * backAlpha

                val height = content.height
                content.translationY = (height * (1 - value))
            }
            doOnEnd { endAction() }
            start()
        }
    }

    private fun withContext(block: (Activity) -> Unit) {
        weakContext.get()?.takeIf { it.isStillAlive() }?.let {
            block(it)
        }
    }
}
