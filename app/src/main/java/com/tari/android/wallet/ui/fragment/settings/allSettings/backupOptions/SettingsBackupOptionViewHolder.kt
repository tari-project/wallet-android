package com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions

import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemSettingsBackupOptionBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.setColor
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState

class SettingsBackupOptionViewHolder(view: ItemSettingsBackupOptionBinding) :
    CommonViewHolder<SettingsBackupOptionViewHolderItem, ItemSettingsBackupOptionBinding>(view) {

    override fun bind(item: SettingsBackupOptionViewHolderItem) {
        super.bind(item)

        ui.leftIcon.setImageResource(item.leftIconId)
        ui.cloudBackupStatusProgressView.setColor(color(R.color.all_settings_back_up_status_processing))
        ui.backUpWalletCtaView.setOnClickListener { item.action.invoke() }
        ui.lastBackupTimeTextView.text = item.lastBackupDate

        item.backupState?.let { activateBackupStatusView(it) }
    }

    private fun activateBackupStatusView(backupState: PresentationBackupState) {
        val iconView = when (backupState.status) {
            PresentationBackupState.BackupStateStatus.InProgress -> ui.cloudBackupStatusProgressView
            PresentationBackupState.BackupStateStatus.Success -> ui.cloudBackupStatusSuccessView
            PresentationBackupState.BackupStateStatus.Warning -> ui.cloudBackupStatusWarningView
            PresentationBackupState.BackupStateStatus.Scheduled -> ui.cloudBackupStatusScheduledView
        }

        fun View.adjustVisibility() {
            visibility = if (this == iconView) View.VISIBLE else View.INVISIBLE
        }

        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        ui.cloudBackupStatusScheduledView.adjustVisibility()
        val hideText = backupState.textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(backupState.textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (backupState.textColor != -1) ui.backupStatusTextView.setTextColor(color(backupState.textColor))
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemSettingsBackupOptionBinding::inflate, SettingsBackupOptionViewHolderItem::class.java) {
                SettingsBackupOptionViewHolder(it as ItemSettingsBackupOptionBinding)
            }
    }
}