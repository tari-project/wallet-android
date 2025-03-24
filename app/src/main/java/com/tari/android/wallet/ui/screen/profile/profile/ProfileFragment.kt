package com.tari.android.wallet.ui.screen.profile.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem

class ProfileFragment : CommonFragment<ProfileViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                TariDesignSystem(viewModel.currentTheme) {
                    ProfileScreen(
                        uiState = uiState,
                        onInviteLinkShareClick = { viewModel.onInviteLinkShareClick() },
                        onStartMiningClicked = { viewModel.onStartMiningClicked() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ProfileViewModel by viewModels()
        bindViewModel(viewModel)
    }
}