package com.tari.android.wallet.ui.screen.settings.backup.backupSettings.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tari.android.wallet.databinding.ViewBackupOptionBinding
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible

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
        ui.title.text = context.getString(backupOptionViewModel.title)
        setPermissionSwitchListener()
        observeViewModel()
    }

    private fun observeViewModel() = with(viewModel) {
        observe(switchChecked) { setSwitchCheck(it) }

        observe(inProgress) { onChangeInProgress(it) }

        observe(openFolderSelection) { backupManager.setupStorage(viewModel.option.value!!.type, fragment) }

        observe(lastSuccessDate) { updateLastSuccessDate(it) }
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

    private fun updateLastSuccessDate(date: String) {
        ui.lastBackupTimeTextView.setVisible(date.isNotBlank(), View.GONE)
        ui.lastBackupTimeTextView.text = date
    }

    override fun setup() = Unit
}