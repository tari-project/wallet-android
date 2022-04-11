package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewBackupOptionBinding
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.setColor

class BackupOptionView : CommonView<BackupOptionViewModel, ViewBackupOptionBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewBackupOptionBinding = ViewBackupOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun init(backupOptionViewModel: BackupOptionViewModel) {
//        ui.title.text = title
    }

    //        ui.googleDriveBackup.init(getString(back_up_wallet_google_title))
//        ui.dropboxBackup.init(getString(back_up_wallet_dropbox_title))


    override fun setup() {
        ui.backupPermissionProgressBar.setColor(color(R.color.back_up_settings_permission_processing))
    }
}