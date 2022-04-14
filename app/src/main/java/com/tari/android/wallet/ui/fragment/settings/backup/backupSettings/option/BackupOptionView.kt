package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewBackupOptionBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.ThrottleClick
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.setColor
import com.tari.android.wallet.ui.extension.setVisible

class BackupOptionView : CommonView<BackupOptionViewModel, ViewBackupOptionBinding> {

    private lateinit var fragment: Fragment

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewBackupOptionBinding = ViewBackupOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun init(fragment: Fragment, backupOptionViewModel: BackupOptionViewModel) {
        this.fragment = fragment
        bindViewModel(backupOptionViewModel)
        ui.backupPermissionProgressBar.setColor(color(R.color.back_up_settings_permission_processing))
        ui.title.text = context.getString(backupOptionViewModel.title)
        setPermissionSwitchListener()
        observeViewModel()
    }

    private fun observeViewModel() = with(viewModel) {
        observe(switchChecked) { setSwitchCheck(it) }

        observe(inProgress) { onChangeInProgress(it) }

        observe(showBackupsWillBeDeletedDialog) { showBackupsWillBeDeletedDialog(it.onAccept, it.onDismiss) }

        observe(openFolderSelection) { backupManager.setupStorage(viewModel.option.value!!.type, fragment) }
    }

    private fun setSwitchCheck(isChecked: Boolean) = with(ui) {
        ui.backupSwitch.setOnCheckedChangeListener(null)
        ui.backupSwitch.isChecked = isChecked
        setPermissionSwitchListener()
    }

    private fun onChangeInProgress(inProgress: Boolean) = with(ui) {
        ui.backupSwitch.setVisible(!inProgress, View.INVISIBLE)
        ui.backupPermissionProgressBar.setVisible(inProgress, View.INVISIBLE)
    }

    private fun setPermissionSwitchListener() {
        ui.backupSwitch.setOnCheckedChangeListener { _, isChecked -> viewModel.onBackupSwitchChecked(isChecked) }
    }

    //todo refactor to nice state
    private fun showBackupsWillBeDeletedDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
        BottomSlideDialog(
            context,
            R.layout.dialog_turn_off_backups_will_be_deleted_warning,
            canceledOnTouchOutside = false
        ).apply {
            findViewById<View>(R.id.backup_turn_off_confirm_button)
                .setOnClickListener(ThrottleClick {
                    onAccept()
                    dismiss()
                })
            findViewById<View>(R.id.backup_turn_off_cancel_button)
                .setOnClickListener(ThrottleClick {
                    onDismiss()
                    dismiss()
                })
        }.show()
    }

    override fun setup() = Unit
}