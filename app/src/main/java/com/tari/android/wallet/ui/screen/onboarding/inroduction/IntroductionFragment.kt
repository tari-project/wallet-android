package com.tari.android.wallet.ui.screen.onboarding.inroduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowFragment
import com.tari.android.wallet.util.extension.collectFlow
import kotlin.getValue

class IntroductionFragment : OnboardingFlowFragment<IntroductionViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val state by viewModel.uiState.collectAsState()

                TariDesignSystem(viewModel.currentTheme) {
                    IntroductionScreen(
                        uiState = state,
                        onImportWalletClick = { viewModel.onWalletRestoreClick() },
                        onCreateWalletClick = { viewModel.onCreateWalletClick() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: IntroductionViewModel by viewModels()
        bindViewModel(viewModel)

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is IntroductionModel.Effect.GoToCreateWallet -> {
                    onboardingListener.continueToCreateWallet()
                }
            }
        }
    }
}