package com.tari.android.wallet.ui.screen.onboarding.activity

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.util.EffectChannelFlow
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowModel.Effect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OnboardingFlowViewModel : CommonViewModel() {

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    fun showResetFlowDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.create_wallet_cancel_dialog_title)),
            BodyModule(resourceManager.getString(R.string.create_wallet_cancel_dialog_description)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                viewModelScope.launch(Dispatchers.Main) {
                    _effect.send(Effect.ResetFlow)
                    hideDialog()
                }
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                hideDialog()
            },
        )
    }

    fun navigateToHome() {
        tariNavigator.navigate(Navigation.Home())
    }
}