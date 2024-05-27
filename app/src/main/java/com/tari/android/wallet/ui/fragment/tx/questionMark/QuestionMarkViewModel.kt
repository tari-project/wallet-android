package com.tari.android.wallet.ui.fragment.tx.questionMark

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs

class QuestionMarkViewModel : CommonViewModel() {
    fun showUniversityDialog() {
        showModularDialog(
            ConfirmDialogArgs(
                title = resourceManager.getString(R.string.home_balance_info_help_title),
                description = resourceManager.getString(R.string.home_balance_info_help_description),
                cancelButtonText = resourceManager.getString(R.string.common_cancel),
                confirmButtonText = resourceManager.getString(R.string.home_balance_info_help_button),
                onConfirm = { _openLink.postValue(resourceManager.getString(R.string.tari_lab_university_url)) },
            ).getModular(resourceManager)
        )
    }
}