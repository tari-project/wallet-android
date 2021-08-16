package com.tari.android.wallet.ui.fragment.tx

import android.app.Dialog
import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomTypefaceSpan
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.string

internal class BackupWalletPrompt private constructor(
    private val dialog: Dialog
) {

    constructor(
        context: Context,
        title: CharSequence,
        description: CharSequence,
        ctaText: CharSequence = context.string(R.string.home_back_up_wallet_back_up_cta),
        dismissText: CharSequence = context.string(R.string.home_back_up_wallet_delay_back_up_cta),
        router: TxListFragment.TxListRouter
    ) : this(
        BottomSlideDialog(
            context,
            R.layout.dialog_backup_wallet_prompt,
            canceledOnTouchOutside = false,
            dismissViewId = R.id.home_backup_wallet_prompt_dismiss_cta_view
        ).apply {
            findViewById<TextView>(R.id.home_backup_wallet_prompt_title_text_view).text = title
            findViewById<TextView>(R.id.home_backup_wallet_prompt_description_text_view).text =
                description
            findViewById<TextView>(R.id.home_backup_wallet_prompt_dismiss_cta_view).text =
                dismissText
            val backupCta =
                findViewById<TextView>(R.id.home_backup_wallet_prompt_backup_cta_view)
            backupCta.text = ctaText
            backupCta.setOnClickListener {
                router.toAllSettings()
                dismiss()
            }
        }.asAndroidDialog()
    )

    constructor(
        context: Context,
        regularTitlePart: CharSequence,
        highlightedTitlePart: CharSequence,
        description: CharSequence,
        ctaText: CharSequence = context.string(R.string.home_back_up_wallet_back_up_cta),
        dismissText: CharSequence = context.string(R.string.home_back_up_wallet_delay_back_up_cta),
        router: TxListFragment.TxListRouter
    ) : this(
        context,
        SpannableStringBuilder().apply {
            val highlightedPart = SpannableString(highlightedTitlePart)
            highlightedPart.setSpan(
                CustomTypefaceSpan("", CustomFont.AVENIR_LT_STD_HEAVY.asTypeface(context)),
                0,
                highlightedPart.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            insert(0, regularTitlePart)
            insert(regularTitlePart.length, " ")
            insert(regularTitlePart.length + 1, highlightedPart)
        },
        description,
        ctaText,
        dismissText,
        router
    )

    fun asAndroidDialog() = dialog

}