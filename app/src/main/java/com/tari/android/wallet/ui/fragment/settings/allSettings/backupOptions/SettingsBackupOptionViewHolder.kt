package com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions

import android.view.View
import com.tari.android.wallet.databinding.ItemSettingsBackupOptionBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState

class SettingsBackupOptionViewHolder(view: ItemSettingsBackupOptionBinding) :
    CommonViewHolder<SettingsBackupOptionViewHolderItem, ItemSettingsBackupOptionBinding>(view) {

    override fun bind(item: SettingsBackupOptionViewHolderItem) {
        super.bind(item)

        ui.leftIcon.setImageResource(item.leftIconId)
        ui.cloudBackupStatusProgressView.setWhite()
        ui.backUpWalletCtaView.setOnClickListener { item.action.invoke() }

        item.backupState?.let { activateBackupStatusView(it) }
    }

    private fun activateBackupStatusView(backupState: PresentationBackupState) {
        val iconView = when (backupState.status) {
            PresentationBackupState.BackupStateStatus.InProgress -> ui.cloudBackupStatusProgressView
            PresentationBackupState.BackupStateStatus.Success -> ui.cloudBackupStatusSuccessView
            PresentationBackupState.BackupStateStatus.Warning -> ui.cloudBackupStatusWarningView
        }

        fun View.adjustVisibility() {
            visibility = if (this == iconView) View.VISIBLE else View.INVISIBLE
        }

        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        val hideText = backupState.textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(backupState.textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (backupState.textColor != -1) ui.backupStatusTextView.setTextColor(itemView.context.colorFromAttribute(backupState.textColor))
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemSettingsBackupOptionBinding::inflate, SettingsBackupOptionViewHolderItem::class.java) {
                SettingsBackupOptionViewHolder(it as ItemSettingsBackupOptionBinding)
            }
    }
}