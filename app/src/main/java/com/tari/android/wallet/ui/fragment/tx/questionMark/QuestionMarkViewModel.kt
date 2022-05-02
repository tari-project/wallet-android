package com.tari.android.wallet.ui.fragment.tx.questionMark

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs

internal class QuestionMarkViewModel : CommonViewModel() {
    fun showUniversityDialog() {
        val confirmDialogArgs = ConfirmDialogArgs(
            resourceManager.getString(R.string.home_balance_info_help_title),
            resourceManager.getString(R.string.home_balance_info_help_description),
            resourceManager.getString(R.string.common_cancel),
            resourceManager.getString(R.string.home_balance_info_help_button),
            onConfirm = { _openLink.postValue(resourceManager.getString(R.string.tari_lab_university_url)) }
        )
        _modularDialog.postValue(confirmDialogArgs.getModular(resourceManager))
    }
}