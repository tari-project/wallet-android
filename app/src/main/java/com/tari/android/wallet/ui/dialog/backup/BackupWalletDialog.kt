package com.tari.android.wallet.ui.dialog.backup

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogBackupWalletPromptBinding
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomTypefaceSpan
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.string

internal class BackupWalletDialog(context: Context, args: BackupWalletDialogArgs) : BottomSlideDialog(
    context,
    R.layout.dialog_backup_wallet_prompt,
    canceledOnTouchOutside = false,
    dismissViewId = R.id.home_backup_wallet_prompt_dismiss_cta_view
) {

    val ui = DialogBackupWalletPromptBinding.bind(dialog.findViewById(R.id.root))

    init {
        val title = SpannableStringBuilder().apply {
            val highlightedPart = SpannableString(args.highlightedTitlePart)
            highlightedPart.setSpan(
                CustomTypefaceSpan("", CustomFont.AVENIR_LT_STD_HEAVY.asTypeface(context)),
                0,
                highlightedPart.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            insert(0, args.regularTitlePart)
            insert(args.regularTitlePart.length, " ")
            insert(args.regularTitlePart.length + 1, highlightedPart)
        }
        ui.homeBackupWalletPromptTitleTextView.text = title
        ui.homeBackupWalletPromptDescriptionTextView.text = args.description
        ui.homeBackupWalletPromptBackupCtaView.text = context.string(args.ctaText)
        ui.homeBackupWalletPromptDismissCtaView.text = context.string(args.dismissText)
        ui.homeBackupWalletPromptBackupCtaView.apply {
            context.string(args.ctaText)
            setOnClickListener {
                args.backupAction()
                dismiss()
            }
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()
}