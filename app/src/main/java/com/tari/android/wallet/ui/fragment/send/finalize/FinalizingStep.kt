package com.tari.android.wallet.ui.fragment.send.finalize

import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.ui.common.domain.ResourceManager

sealed class FinalizingStep(val resourceManager: ResourceManager, descLine1Res: Int, descLine2Res: Int) {

    val descLine1 = resourceManager.getString(descLine1Res)
    val descLine2 = resourceManager.getString(descLine2Res)

    class ConnectionCheck(resourceManager: ResourceManager) :
        FinalizingStep(resourceManager, finalize_send_tx_sending_step_1_desc_line_1, finalize_send_tx_sending_step_1_desc_line_2)

    class Discovery(resourceManager: ResourceManager) :
        FinalizingStep(resourceManager, finalize_send_tx_sending_step_2_desc_line_1, finalize_send_tx_sending_step_2_desc_line_2)

    class Sent(resourceManager: ResourceManager) :
        FinalizingStep(resourceManager, finalize_send_tx_sending_step_3_desc_line_1, finalize_send_tx_sending_step_3_desc_line_2)
}