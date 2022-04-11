package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewBackupOptionBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.setColor

class BackupOptionView : CommonView<CommonViewModel, ViewBackupOptionBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewBackupOptionBinding = ViewBackupOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun setup() {
        ui.backupPermissionProgressBar.setColor(color(R.color.back_up_settings_permission_processing))
    }
}