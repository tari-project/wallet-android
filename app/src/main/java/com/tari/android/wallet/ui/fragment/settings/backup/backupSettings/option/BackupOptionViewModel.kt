package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions

class BackupOptionViewModel() : CommonViewModel() {

    private val _option = MutableLiveData<BackupOptions>()
    val option: LiveData<BackupOptions> = _option

//    private val _isEnabled

    fun setup(option: BackupOptions) {
        _option.postValue(option)
    }
}