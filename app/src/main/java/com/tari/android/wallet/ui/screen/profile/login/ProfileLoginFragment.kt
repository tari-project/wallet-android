package com.tari.android.wallet.ui.screen.profile.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem

class ProfileLoginFragment : CommonFragment<ProfileLoginViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                TariDesignSystem(viewModel.currentTheme) {
                    ProfileLoginScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ProfileLoginViewModel by viewModels()
        bindViewModel(viewModel)
    }
}