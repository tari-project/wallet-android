package com.tari.android.wallet.ui.screen.restore.chooseRestoreOption.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewRestoreOptionBinding
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setVisible

class RecoveryOptionView : CommonView<RecoveryOptionViewModel, ViewRestoreOptionBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewRestoreOptionBinding = ViewRestoreOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun init(option: BackupOption) {
        val text = when (option) {
            BackupOption.Google -> R.string.back_up_wallet_restore_with_google_drive
            BackupOption.Local -> R.string.back_up_wallet_restore_with_local_files
//            BackupOption.Dropbox -> R.string.back_up_wallet_restore_with_dropbox // FIXME: Dropbox backup is not supported yet
        }
        ui.title.text = context.getString(text)
        bindViewModel(RecoveryOptionViewModel().apply { this.option = option })
    }

    fun updateLoading(isLoading: Boolean) {
        ui.restoreWalletMenuItemProgressView.setVisible(isLoading)
        ui.restoreWalletMenuItemArrowImageView.setVisible(!isLoading)
        ui.restoreWalletCtaView.isEnabled = !isLoading
    }

    override fun setup() {
        ui.restoreWalletMenuItemProgressView.gone()
    }
}