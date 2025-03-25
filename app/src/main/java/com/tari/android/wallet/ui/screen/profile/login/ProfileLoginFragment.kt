package com.tari.android.wallet.ui.screen.profile.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class ProfileLoginFragment : CommonFragment<ProfileLoginViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        TariDesignSystem(viewModel.currentTheme) {
            ProfileLoginScreen(
                authUrl = viewModel.authUrl
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ProfileLoginViewModel by viewModels()
        bindViewModel(viewModel)
    }
}