package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.coroutineScope
import com.tari.android.wallet.databinding.ViewBackupOptionBinding
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionModel.Effect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class BackupOptionView : CommonView<BackupOptionViewModel, ViewBackupOptionBinding>, LifecycleObserver {

    private lateinit var fragment: Fragment
    private lateinit var lifecycleScope: LifecycleCoroutineScope

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
        fragment.lifecycle.addObserver(this)
        lifecycleScope = fragment.lifecycle.coroutineScope
        bindViewModel(backupOptionViewModel)
        ui.title.text = context.getString(backupOptionViewModel.title)
        setPermissionSwitchListener()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is Effect.SetupStorage -> effect.backupManager.setupStorage(effect.optionType, fragment)
                }
            }

            viewModel.uiState.filterNotNull().collect { state ->
                setSwitchCheck(state.switchChecked)
                onChangeInProgress(state.loading)
                updateLastSuccessDate(state.lastSuccessDate)
            }
        }
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

    private fun updateLastSuccessDate(date: String?) {
        ui.lastBackupTimeTextView.setVisible(!date.isNullOrBlank(), View.GONE)
        ui.lastBackupTimeTextView.text = date
    }

    override fun setup() = Unit
}